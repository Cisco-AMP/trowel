package com.cisco.amp.trowel

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.testkit.ScalatestRouteTest

import spray.json._

class StatsServerSpec extends WordSpec with Matchers with ScalatestRouteTest with JsonFormatDefinitions  {
  "StatsServer" should {
    "should return valid json with correct fields" in {
      Get("/stats") ~> StatsServer.getRoutes ~> check {
        Stats.reset()

        val response = responseAs[String].parseJson.convertTo[AmqpTransferStats]

        status shouldEqual StatusCodes.OK
        response.app.uptime shouldBe a [Long]

        response.sink.acks shouldBe 0
        response.sink.rejects shouldBe 0
        response.sink.dropped shouldBe 0
      }
    }
  }
}
