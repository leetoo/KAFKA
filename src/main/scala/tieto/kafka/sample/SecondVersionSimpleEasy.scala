import akka.actor.ActorSystem
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka._
import akka.stream.ActorMaterializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}

object KafkaConsumer extends App {

  implicit val actorSystem = ActorSystem("test-actor-system")
  implicit val streamMaterializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  implicit var committableOffset : ConsumerMessage.CommittableOffset = committableOffset
  val log = actorSystem.log


  // PRODUCER config
  val producerSettings = ProducerSettings(
    actorSystem,
    new ByteArraySerializer,
    new StringSerializer)
    .withBootstrapServers("localhost:9092")
    .withProperty("auto.create.topics.enable", "true")

  // CONSUMER config
  val consumerSettings = ConsumerSettings(
    system = actorSystem,
    keyDeserializer = new ByteArrayDeserializer,
    valueDeserializer = new StringDeserializer)
    .withBootstrapServers("localhost:9092")
    .withGroupId("kafka-sample")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  // ROUTE OF THE APP
  Consumer.committableSource(consumerSettings,
    Subscriptions.topics("topic1"))
    .mapConcat {
      msg =>
        committableOffset = msg.committableOffset
        msg.record.value().split(" ").toList }
    .map {
      msg => println(s"topic1 -> topic2: $msg")
        ProducerMessage.Message(new ProducerRecord[Array[Byte], String]( "topic2", msg), committableOffset)
    }
    .runWith(Producer.commitableSink(producerSettings))
}