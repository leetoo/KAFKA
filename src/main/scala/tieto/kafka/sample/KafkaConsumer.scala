package tieto.kafka.sample

import akka.actor.ActorSystem
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.{Producer, ProducerRecord}
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}

/**
  * Created by cfernandes on 2016-09-26.
  */
object KafkaConsumer extends App {

  implicit val actorSystem = ActorSystem("test-actor-system")
  implicit val streamMaterializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  val log = actorSystem.log


  // PRODUCER
  val producerSettings = ProducerSettings(
    actorSystem,
    new StringSerializer,
    new StringSerializer)
    .withBootstrapServers("localhost:9092")
    .withProperty("auto.create.topics.enable", "true")


  // CONSUMER
  val consumerSettings = ConsumerSettings(
    system = actorSystem,
    keyDeserializer = new ByteArrayDeserializer,
    valueDeserializer = new StringDeserializer)
    .withBootstrapServers("localhost:9092")
    .withGroupId("tieto-kafka-sample")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
  // -----------------------------------------------------------------------//

  // ROUTE OF THE APP
  Consumer.committableSource(consumerSettings, Subscriptions.topics("LineOfWords"))
    .mapConcat( msg => ) {
      msg => println(s"topic1 -> topic2: $msg")
        val newMSG = msg.toString.split("\\s+")
      newMSG.foreach( i => ProducerMessage.Message(new ProducerRecord[String, String]( "SeparatedWords", i), msg.committableOffset))
      ProducerMessage.Message(new ProducerRecord[String, String]( "SeparatedWords", ), msg.committableOffset)
    }
    .runWith(Producer.commitableSink(producerSettings))
}