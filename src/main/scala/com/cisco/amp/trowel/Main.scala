package com.cisco.amp.trowel

import java.io.File


import akka.actor.ActorSystem

import pureconfig._
import pureconfig.generic.auto._ //do not remove this import

import scala.concurrent.ExecutionContext


object Main extends App {
  val ApplicationName = "trowel"

  var config: ConfigReader.Result[TrowelConfig] = _
  val parser = new scopt.OptionParser[Unit](ApplicationName) {
    head(ApplicationName)
    help('h', "help")
    opt[File]('c', "config").required.valueName("<file>").foreach { file =>
      config = ConfigSource.file(file).load[TrowelConfig]
    }
  }

  if (!parser.parse(args)) {
    System.exit(1)
  }

  config match {
    case Right(c) => {
      implicit val actorSystem: ActorSystem = ActorSystem(ApplicationName)
      implicit val executionContext: ExecutionContext = actorSystem.dispatcher

      AmqpTransfer(c).start()
      StatsServer.initiate(c.statsServer)
    }
    case Left(e) => {
      println(s"Could not load the config: ${e.head}")
      System.exit(1)
    }
  }
}