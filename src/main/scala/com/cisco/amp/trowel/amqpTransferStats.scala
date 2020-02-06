package com.cisco.amp.trowel

import com.cisco.amp.trowel.amqp.AmqpSourceStats

case class AmqpTransferStats(app: ApplicationStats, source: AmqpSourceStats, sink: AmqpTransferSinkStats)
case class AmqpTransferSinkStats(acks: Long, rejects: Long, dropped: Long)