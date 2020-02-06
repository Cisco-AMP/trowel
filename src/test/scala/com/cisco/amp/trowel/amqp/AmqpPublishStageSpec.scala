package akka.stream.alpakka.amqp.impl

import java.io.IOException
import java.util.concurrent.TimeoutException

import akka.Done
import akka.actor.ActorSystem
import akka.stream.alpakka.amqp.{AmqpConnectionProvider, AmqpWriteSettings, ReadResult}
import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult
import akka.util.ByteString
import com.cisco.amp.trowel.amqp.AmqpPublishStage
import com.cisco.amp.trowel.{AmqpTransferResult, UnconfirmedPublishError}
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{Channel, Connection, Envelope, ShutdownSignalException}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{spy, when, verify}
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar._

import scala.concurrent.Future

class ccc(readResult: ReadResult) extends CommittableReadResult {
  override val message: ReadResult = readResult
  override def ack(multiple: Boolean): Future[Done] = Future.successful(Done)
  override def nack(multiple: Boolean, requeue: Boolean): Future[Done] = Future.successful(Done)
}

class AmqpPublishStageSpec extends WordSpec with Matchers {

  implicit val actorSystem: ActorSystem = ActorSystem("AmqpPublishStageSpec")

  val exchange = "change"
  val routingKey = "routingKey"

  val readResultObj = ReadResult(
    ByteString("hallo"),
    new Envelope(1L, true, exchange, routingKey),
    new BasicProperties.Builder().deliveryMode(2).build()
  )

  private val committableReadResult = new CommittableReadResult {
    override val message: ReadResult = readResultObj
    override def ack(multiple: Boolean): Future[Done] = Future.successful(Done)
    override def nack(multiple: Boolean, requeue: Boolean): Future[Done] = Future.failed(new RuntimeException("whooopwippg"))
  }

  private def getMockPublisher(channel: Channel): AmqpPublishStage = {
    val mockConnectionProvider = mock[AmqpConnectionProvider]
    val mockConnection = mock[Connection]
    when(mockConnectionProvider.get).thenReturn(mockConnection)
    when(mockConnection.createChannel).thenReturn(channel)

    val amqpWriteSettings: AmqpWriteSettings = AmqpWriteSettings(mockConnectionProvider)
      .withExchange(exchange)
      .withRoutingKey(routingKey)
    new AmqpPublishStage(amqpWriteSettings)
  }

  "AmqpPublishStage" should {
    "return a Success[AmqpTransferResult] with None when amqp confirms receipt" in {
      val mockChannel = mock[Channel]
      when(mockChannel.waitForConfirms(any())).thenReturn(true)
      val amqpPublishStage = getMockPublisher(mockChannel)

      val result = amqpPublishStage.publish(committableReadResult)
      result shouldBe AmqpTransferResult(committableReadResult, None)
    }

    "return a Success[AmqpTransferResult] with Some[Throwable] when publisher does not confirm successful publish" in {
      val mockChannel = mock[Channel]
      when(mockChannel.waitForConfirms(any())).thenReturn(false)
      val amqpPublishStage = getMockPublisher(mockChannel)

      val result = amqpPublishStage.publish(committableReadResult)
      result.readResult shouldBe committableReadResult
      result.publishError match {
        case Some(e) => e shouldBe a [UnconfirmedPublishError]
        case None    => fail("Expected an UnconfirmedPublishError to be set.")
      }
    }

    "return an unsuccessful publish when amqp errors" in {
      val mockChannel = mock[Channel]
      val ioMessage = "some io exception"
      when(mockChannel.basicPublish(any[String], any[String], any[Boolean], any[BasicProperties], any())).thenThrow(new IOException(ioMessage))
      val amqpPublishStage = getMockPublisher(mockChannel)

      val res = amqpPublishStage.publish(committableReadResult)
      res.publishError match {
        case Some(e) => e shouldBe a [IOException]
        case None    => fail("Expected an IOException to be set.")
      }
    }

    "return an unsuccessful publish when waiting on confirmation times out" in {
      val mockChannel = mock[Channel]
      val timeoutMessage = "some timeout exception"
      when(mockChannel.waitForConfirms(any())).thenThrow(new TimeoutException(timeoutMessage))
      val amqpPublishStage = getMockPublisher(mockChannel)

      val res = amqpPublishStage.publish(committableReadResult)
      res.publishError match {
        case Some(e) => e shouldBe a [TimeoutException]
        case None    => fail(s"Expected a TimeoutException to be returned.")
      }
    }

    "restarts the AMQP connection when the connection throws a shutdown error" in {
      val mockChannel = mock[Channel]
      val shutdownMessage = "IOException"
      when(mockChannel.basicPublish(any(),any(),any(),any(), any()))
        .thenThrow(new java.io.IOException(shutdownMessage))
      val amqpPublishStage = spy(getMockPublisher(mockChannel))

      val res = amqpPublishStage.publish(committableReadResult)
      verify(amqpPublishStage).resetChannel()
      res.publishError match {
        case Some(e) => e shouldBe a [IOException]
        case None    => fail(s"Expected IOException to be returned.")
      }
    }
  }
}
