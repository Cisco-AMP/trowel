package com.cisco.amp.trowel

import java.util.concurrent.atomic.LongAdder

import akka.Done
import akka.event.Logging
import akka.stream.{Graph, SinkShape}
import akka.stream.scaladsl.Sink

import scala.concurrent.Future

object AmqpTransferSink extends TrowelLogger {


  private val ackCount: LongAdder = new LongAdder
  private val droppedCount: LongAdder = new LongAdder
  private val rejectCount: LongAdder = new LongAdder

  def build: Graph[SinkShape[AmqpTransferResult], Future[Done]] = {
    Sink.foreach[AmqpTransferResult](ackOrNack)
  }

  private def ackOrNack(transferResult: AmqpTransferResult): Unit = {
    transferResult.publishError match {
      case Some(error) => {
        log(s"Error when publishing: $error", Logging.WarningLevel)

        transferResult.readResult.nack(multiple = false, requeue = true)
        rejectCount.increment()
      }
      case None => {
        transferResult.readResult.ack()
        ackCount.increment()
      }
    }
  }

  def resetStats(): Unit = {
    ackCount.reset()
    rejectCount.reset()
    droppedCount.reset()
  }

  def stats: AmqpTransferSinkStats = AmqpTransferSinkStats(
    ackCount.longValue(),
    rejectCount.longValue(),
    droppedCount.longValue()
  )
}
