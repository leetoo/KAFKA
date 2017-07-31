package tieto.kafka.sample

import java.util.Properties
import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.{CommittableMessage, CommittableOffset}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.{ActorMaterializer, SourceShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Source, Zip}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}

import scala.concurrent.Future
import scala.util.Success
import scala.concurrent.duration._

object FirstVersionHandMade extends App {

  implicit val actorSystem = ActorSystem("test-actor-system")
  implicit val streamMaterializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  val log = actorSystem.log

  // PRODUCER config
  val producerSettings = ProducerSettings(actorSystem,
    new ByteArraySerializer,
    new StringSerializer)
    .withBootstrapServers("localhost:9092")
    .withProperty("auto.create.topics.enable", "true")

  // CONSUMER config
  val consumerSettings =
    ConsumerSettings(system = actorSystem,
      keyDeserializer = new ByteArrayDeserializer,
      valueDeserializer = new StringDeserializer)
      .withBootstrapServers("localhost:9092")
      .withGroupId("kafka-sample")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  implicit val producerConfig = {
    val props = new Properties()
    props.setProperty("bootstrap.servers", "localhost:9092")
    props.setProperty("key.serializer", classOf[StringSerializer].getName)
    props.setProperty("value.serializer", classOf[StringSerializer].getName)
    props
  }

  lazy val kafkaProducer = new KafkaProducer[String, String](producerConfig)

  // Create Scala future from Java
  private def publishToKafka(id: String, data: String) = {
    Future {
      kafkaProducer
        .send(new ProducerRecord("wordSeparated", id, data))
        .get()
    }
  }

  def getKafkaSource =
    Consumer
      .committableSource(consumerSettings, Subscriptions.topics("lineOfWords"))
      // It consumes 10 messages or waits 30 seconds to push downstream
      .groupedWithin(10, 30 seconds)

  val getStreamSource = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._
    val in = getKafkaSource
    // BroadCast to two flows. One for obtain the last offset to commit
    // and other to return the Seq with the words to publish
    val br = b.add(Broadcast[Seq[CommittableMessage[Array[Byte], String]]](2))
    val zipResult = b.add(Zip[CommittableOffset, Array[String]]())
    val flowCommit = Flow[Seq[CommittableMessage[Array[Byte], String]]].map(_.last.committableOffset)
    // Flow that creates the list of all words in all consumed messages
    val _flowWords =
      Flow[Seq[CommittableMessage[Array[Byte], String]]].map(input => {
        input.map(_.record.value()).mkString(" ").split(" ")
      })
    val zip = Zip[CommittableOffset, Array[String]]
    // build the Stage
    in ~> br ~> flowCommit ~> zipResult.in0
    br ~> _flowWords ~> zipResult.in1
    SourceShape(zipResult.out)
  }
  Source.fromGraph(getStreamSource).runForeach { msgs =>
  {
    // Publish all words and when all futures complete the commit the last Kafka offset
    val futures = msgs._2.map(publishToKafka("wordSeparated", _)).toList
    // Produces in parallel!!. Use flatMap to make it in order
    Future.sequence(futures).onComplete {
      case Success(e) => {
        msgs._1.commitScaladsl()
      }
    }
  }
  }
}