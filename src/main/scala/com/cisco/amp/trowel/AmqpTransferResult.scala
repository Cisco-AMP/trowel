package com.cisco.amp.trowel

import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult

case class AmqpTransferResult(readResult: CommittableReadResult, publishError: Option[Throwable])
