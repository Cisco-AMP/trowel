package com.cisco.amp.trowel.amqp

import akka.stream.alpakka.amqp.{AmqpCredentials, AmqpDetailsConnectionProvider}
import com.cisco.amp.trowel.Credentials

object AmqpConnectionFactory {

  def getConnectionProvider(host: String, port: Int, credentials: Option[Credentials]): AmqpDetailsConnectionProvider  = {
    val baseConnection = AmqpDetailsConnectionProvider(host, port)
      .withAutomaticRecoveryEnabled(false)
      .withTopologyRecoveryEnabled(false)

    credentials match {
      case Some(creds) => baseConnection.withCredentials(AmqpCredentials(creds.username, creds.password))
      case None        => baseConnection.withCredentials(AmqpCredentials("guest", "guest"))
    }
  }
}
