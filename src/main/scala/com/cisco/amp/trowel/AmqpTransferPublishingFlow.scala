package com.cisco.amp.trowel

import akka.NotUsed
import akka.stream.alpakka.amqp.AmqpWriteSettings
import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult
import akka.stream.scaladsl.Flow
import com.cisco.amp.trowel.amqp.{AmqpConnectionFactory, AmqpPublishStage}

object AmqpTransferPublishingFlow {
  def build(amqTargetConfig: AmqTargetConfig): Flow[CommittableReadResult, AmqpTransferResult, NotUsed] = {
    val connectionProvider = AmqpConnectionFactory.getConnectionProvider(
      amqTargetConfig.amq.host,
      amqTargetConfig.amq.port,
      amqTargetConfig.credentials
    )

    Flow.fromGraph(
      new AmqpPublishStage(AmqpWriteSettings(connectionProvider).withExchange(amqTargetConfig.exchange))
    )
  }
}
