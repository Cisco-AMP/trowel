package com.cisco.amp.trowel

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult
import akka.stream.scaladsl.{Flow, RestartFlow, RestartSource, Source}
import com.cisco.amp.trowel.amqp.AmqpConsumer

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object AmqpTransfer {
  def apply(conf: TrowelConfig)(implicit ec: ExecutionContext, mat: Materializer, actorSystem: ActorSystem): AmqpTransfer = {
    val consumer = AmqpConsumer(conf.amqSource)
    consumer.enableLogging(actorSystem)

    new AmqpTransfer(conf, consumer)
  }
}

class AmqpTransfer(conf: TrowelConfig, consumer: AmqpConsumer)
                  (implicit ec: ExecutionContext, mat: Materializer, actorSystem: ActorSystem) extends TrowelLogger  {
  enableLogging(actorSystem)

  val source: Source[CommittableReadResult, NotUsed] = {
    RestartSource.withBackoff(conf.reconnectInterval.min.seconds, conf.reconnectInterval.max.seconds, 0.2, Int.MaxValue) { () =>
      consumer.source
    }
  }

  val flow: Flow[CommittableReadResult, AmqpTransferResult, NotUsed] = {
    RestartFlow.withBackoff(conf.reconnectInterval.min.seconds, conf.reconnectInterval.max.seconds, 0.2, Int.MaxValue) { () =>
      AmqpTransferPublishingFlow.build(conf.amqTarget)
    }
  }

  /**
   * Continuously move messages from the AMQP source queues to the target exchange.
   * Message acking to the source is done after successful write to the target.
   * If an error occurs the entire stream is restarted, and messages unacked to the source should be requeued.
   */
  def start(): Unit = {
    log("Starting AmqpTransfer")
    AmqpTransferSink.enableLogging(actorSystem)
    source
      .via(flow)
      .runWith(AmqpTransferSink.build)
  }
}