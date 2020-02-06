# Trowel
A standalone application to move messages from AMQP source queues to a target exchange

# Running
### With specific config with sbt:
`sbt "run --config \<config file>"`
### With specific config with a jar:
`java -jar \<trowel jar> --config \<config file>` 

## Packaging
`sbt clean assembly`

A jar file will be generated in `/target/scalaVersion`

# Testing
sbt test
