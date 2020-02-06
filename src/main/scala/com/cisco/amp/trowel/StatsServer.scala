package com.cisco.amp.trowel

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, server}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._

import scala.concurrent.Future

object StatsServer extends JsonFormatDefinitions {
  val DefaultBindTo = "127.0.0.1"
  val DefaultPort = 4040

  /**
   * Returns a ServerBinding that can be used for a graceful shutdown:
   * https://doc.akka.io/docs/akka-http/current/server-side/graceful-termination.html
   */
  def initiate(statsServerConfig: StatsServerConfig)(implicit actorSystem: ActorSystem): Future[Http.ServerBinding] = {
    val bindTo: String = statsServerConfig.bindTo match {
      case Some(ip) => ip
      case None => StatsServer.DefaultBindTo
    }

    val port: Int = statsServerConfig.port match {
      case Some(ip) => ip
      case None => StatsServer.DefaultPort
    }

    Http().bindAndHandle(getRoutes, bindTo, port)
  }

  def getRoutes: server.Route =  {
    val stats = Stats
    path("stats") {
      get {
        complete(stats.amqpTransferStats)
      }
    }
  }
}
