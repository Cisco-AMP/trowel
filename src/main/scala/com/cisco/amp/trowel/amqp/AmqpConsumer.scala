package com.cisco.amp.trowel.amqp

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

import akka.NotUsed
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl.{AmqpSource, CommittableReadResult}
import akka.stream.scaladsl.Source
import com.cisco.amp.trowel.{AmqConfig, AmqSourceConfig, Credentials, TrowelLogger}

import scala.collection._
import scala.jdk.CollectionConverters._

object AmqpConsumer {
  private val DefaultNumPrefetch = 10

  private val queueCounts = new ConcurrentHashMap[String, LongAdder]

  def apply(amqSourceConfig: AmqSourceConfig): AmqpConsumer = {
    val amqConfig: AmqConfig = amqSourceConfig.amq
    val numPrefetch = amqSourceConfig.numPrefetch.getOrElse(DefaultNumPrefetch)
    new AmqpConsumer(amqConfig.host, amqConfig.port, amqSourceConfig.queues, amqSourceConfig.credentials, numPrefetch)
  }

  def stats(): AmqpSourceStats = {
    AmqpSourceStats(queueCounts.asScala.view.mapValues{_.longValue}.toMap)
  }
}

/**
 * Reads from 1 or more queues and feeds the messages into a single [[akka.stream.scaladsl.Source]]
 */
class AmqpConsumer(host: String, port: Int, queues: Seq[String], credentials: Option[Credentials], numPrefetch: Int) extends TrowelLogger {
  private val connectionProvider = AmqpConnectionFactory.getConnectionProvider(host, port, credentials)
  private val namedQueueSettings = queues.map { NamedQueueSourceSettings(connectionProvider, _).withAckRequired(true) }
  private val adderSupplier = new java.util.function.Function[String, LongAdder]() {
    override def apply(t: String): LongAdder = new LongAdder()
  } // This is needed because scala doesn't support lambda conversions without a scary-sounding -Xexperimental flag

  val source: Source[CommittableReadResult, NotUsed] = {
    namedQueueSettings.map { namedQueueSetting =>
      AmqpSource.committableSource(namedQueueSetting, numPrefetch).map { ns =>
        AmqpConsumer.queueCounts.computeIfAbsent(namedQueueSetting.queue, adderSupplier).increment()
        ns
      }
    }.foldLeft(Source.empty[CommittableReadResult])( _ merge _ )
  }

  /**
   * Brings down the underlying connections used by this consumer.
   */
  def down(): Unit = {
    log("Shutting down AMQP Source")
    namedQueueSettings.foreach(_.connectionProvider.get.abort())
  }
}
