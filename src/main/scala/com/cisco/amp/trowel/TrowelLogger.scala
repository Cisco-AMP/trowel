package com.cisco.amp.trowel

import akka.actor.ActorSystem
import akka.event.Logging.LogLevel
import akka.event.{LogSource, Logging, LoggingAdapter}

trait TrowelLogger {
  private var logger: Option[LoggingAdapter] = None

  implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
    def genString(o: AnyRef): String = o.getClass.getName
    override def getClazz(o: AnyRef): Class[_] = o.getClass
  }

  def enableLogging(actorSystem: ActorSystem): Unit = {
    if (logger.isEmpty) {
      logger = Some(Logging(actorSystem, this))
    }
  }

  def log(message: String, level: LogLevel = Logging.InfoLevel): Unit = {
    logger match {
      case Some(logger) => logger.log(level, message);
      case None => // Do nothing
    }
  }
}
