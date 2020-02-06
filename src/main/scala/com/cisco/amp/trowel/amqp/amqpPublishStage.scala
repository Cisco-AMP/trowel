package com.cisco.amp.trowel.amqp

import akka.event.Logging
import akka.stream._
import akka.stream.alpakka.amqp.{AmqpConnectionProvider, AmqpWriteSettings}
import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

import com.cisco.amp.trowel.{AmqpTransferResult, TrowelLogger, UnconfirmedPublishError}
import com.rabbitmq.client._


class AmqpPublishStage(settings: AmqpWriteSettings)
  extends GraphStage[FlowShape[CommittableReadResult, AmqpTransferResult]] with TrowelLogger {
  private val connectionShutdownTimeoutSeconds = 10

  private val connectionProvider: AmqpConnectionProvider = settings.connectionProvider
  private val connection: Connection = connectionProvider.get
  private var channel: Channel = connection.createChannel()
  private val exchange: String = settings.exchange.getOrElse("")
  private val routingKey: String = settings.routingKey.getOrElse("")

  val in = Inlet[CommittableReadResult]("AmqpPublish.in")
  val out = Outlet[AmqpTransferResult]("AmqpPublish.out")

  override val shape = FlowShape.of(in, out)

  override def createLogic(attr: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      channel.confirmSelect()

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val elem = grab(in)
          val transferResult: AmqpTransferResult = publish(elem)
          push(out, transferResult)
        }
        override def onUpstreamFinish(): Unit = {
          super.onUpstreamFinish()
          shutdown
        }
        override def onUpstreamFailure(ex: Throwable): Unit = {
          super.onUpstreamFailure(ex)
          shutdown
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          pull(in)
        }
        override def onDownstreamFinish(cause: Throwable): Unit = {
          super.onDownstreamFinish(cause)
          shutdown
        }
      })
    }

  def publish(elem: CommittableReadResult): AmqpTransferResult = {
    try {
      channel.basicPublish(
        exchange,
        routingKey,
        true, // mandatory = true
        elem.message.properties,
        elem.message.bytes.toArray
      )
      val confirmed = channel.waitForConfirms(3000)

      if (confirmed)
        AmqpTransferResult(elem, None)
      else
        throw new UnconfirmedPublishError
    } catch {
      case e @ (_:ShutdownSignalException | _:java.io.IOException) => { // Errors thrown when channel shuts down during publish
        log(s"Attempting to reconnect AMQP channel due to exception: $e", Logging.WarningLevel)
        resetChannel()
        AmqpTransferResult(elem, Some(e))
      }
      case e: Throwable => AmqpTransferResult(elem, Some(e))
    }
  }

  def shutdown(): Unit = connectionProvider.get.close(connectionShutdownTimeoutSeconds)

  def resetChannel(): Unit = {
    try {
      channel.abort
      channel = connection.createChannel()
    } catch {
      case e: java.io.IOException => {
        log(s"Error in aborting AMQP publishing channel: $e", Logging.ErrorLevel)
        throw e
      }
    }
  }

}