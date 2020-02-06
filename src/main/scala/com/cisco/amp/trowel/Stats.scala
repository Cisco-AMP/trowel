package com.cisco.amp.trowel

import com.cisco.amp.trowel.amqp.{AmqpConsumer, AmqpSourceStats}

object Stats {
  private var applicationStartTime = System.currentTimeMillis()

  def amqpSourceStats: AmqpSourceStats = AmqpConsumer.stats()

  def amqpTransferStats: AmqpTransferStats = AmqpTransferStats(applicationStats, amqpSourceStats, amqpTransferSinkStats)

  def amqpTransferSinkStats: AmqpTransferSinkStats = AmqpTransferSink.stats

  def applicationStats: ApplicationStats = ApplicationStats(
    (System.currentTimeMillis() - applicationStartTime) / 1000
  )

  def reset(): Unit = {
    applicationStartTime = System.currentTimeMillis()
    AmqpTransferSink.resetStats()
  }

}
