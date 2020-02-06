package com.cisco.amp.trowel

import java.io.IOError
import java.lang.Error

import akka.Done
import akka.actor.ActorSystem
import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult
import akka.stream.scaladsl.Source
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.verify
import org.mockito.Mockito.times

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._



class AmqpTransferSinkSpec extends WordSpec with Matchers with MockitoSugar with BeforeAndAfterEach {
  implicit val actorSystem: ActorSystem = ActorSystem("AmqTransferSink")

  override def beforeEach() {
    AmqpTransferSink.resetStats()
  }

  "AmqTransferSink" should {
    "calls ack on CommittableReadResult when there were no Errors" in {
      val amqTransferResult = AmqpTransferResult(mock[CommittableReadResult], None)
      val streamFuture: Future[Done] = Source(Seq(amqTransferResult)).runWith(AmqpTransferSink.build)

      Await.result(streamFuture, 3.seconds)
      verify(amqTransferResult.readResult, times(1)).ack()
      verify(amqTransferResult.readResult, times(0)).nack()
      AmqpTransferSink.stats.acks shouldEqual 1
      AmqpTransferSink.stats.rejects shouldEqual 0
      AmqpTransferSink.stats.dropped shouldEqual 0
    }

    "calls nack on CommittableReadResult when there is an UnconfirmedPublishError" in {
      val amqTransferResult = AmqpTransferResult(mock[CommittableReadResult], Some(new UnconfirmedPublishError()))
      val streamFuture: Future[Done] = Source(Seq(amqTransferResult)).runWith(AmqpTransferSink.build)

      Await.result(streamFuture, 3.seconds)
      verify(amqTransferResult.readResult, times(0)).ack()
      verify(amqTransferResult.readResult, times(1)).nack(multiple = false, requeue = true)
      AmqpTransferSink.stats.acks shouldEqual 0
      AmqpTransferSink.stats.rejects shouldEqual 1
      AmqpTransferSink.stats.dropped shouldEqual 0
    }

    "calls nack on CommittableReadResult when there is an IOError" in {
      val amqTransferResult = AmqpTransferResult(mock[CommittableReadResult], Some(new IOError(new Error("I did not try to connect"))))
      val streamFuture: Future[Done] = Source(Seq(amqTransferResult)).runWith(AmqpTransferSink.build)

      Await.result(streamFuture, 3.seconds)
      verify(amqTransferResult.readResult, times(0)).ack()
      verify(amqTransferResult.readResult, times(1)).nack(multiple = false, requeue = true)
      AmqpTransferSink.stats.acks shouldEqual 0
      AmqpTransferSink.stats.rejects shouldEqual 1
      AmqpTransferSink.stats.dropped shouldEqual 0
    }
  }

}
