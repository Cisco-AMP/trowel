package com.cisco.amp.trowel

final case class TrowelConfig(amqSource: AmqSourceConfig, amqTarget: AmqTargetConfig, statsServer: StatsServerConfig, reconnectInterval: ReconnectInterval)
final case class AmqSourceConfig(amq: AmqConfig, queues: Seq[String], credentials: Option[Credentials], numPrefetch: Option[Int])
final case class AmqTargetConfig(amq: AmqConfig, exchange: String, credentials: Option[Credentials])
final case class AmqConfig(host: String, port: Int)
final case class Credentials(username: String, password: String)

final case class StatsServerConfig(bindTo: Option[String], port: Option[Int])

final case class ReconnectInterval(min: Int, max: Int)