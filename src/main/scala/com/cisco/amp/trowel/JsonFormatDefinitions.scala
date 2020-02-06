package com.cisco.amp.trowel

import com.cisco.amp.trowel.amqp.AmqpSourceStats
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait JsonFormatDefinitions extends DefaultJsonProtocol {
  implicit val applicationStatsFormat: RootJsonFormat[ApplicationStats] = jsonFormat1(ApplicationStats)
  implicit val amqSourceStatsFormat: RootJsonFormat[AmqpSourceStats] = jsonFormat1(AmqpSourceStats)
  implicit val amqTransferSinkStatsFormat: RootJsonFormat[AmqpTransferSinkStats] = jsonFormat3(AmqpTransferSinkStats)
  implicit val amqTransferStatsFormat: RootJsonFormat[AmqpTransferStats] = jsonFormat3(AmqpTransferStats)
}
