# Scala Consumer & Producer 
Scala applicaton to get familiar with the workflow of Apache Kafka. Idea of app:

Kafka topic: "topic1" ~> Scala's magic :wink: (split lines of word to single words) ~> Kafka topic "topic2" 

## You can find two different versions: 

* First - We create the pipeline of app on our own. We specify all of the settings etc.
* Second - Much easier based on akka-stream mainly.


## CHEATSHEET

* start zookeeper: bin\windows\zookeeper-server-start.bat config\zookeeper.properties
* start kafka: bin\windows\kafka-server-start.bat config\server.properties
* list topics: bin\windows\kafka-topics.bat --list --zookeeper localhost:2181
* create topic: bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic {name of your topic}
* send messages: bin\kafka-console-producer.bat --broker-list localhost:9092 --topic {name of your topic}
* view messages in topic: bin\windows\kafka-console-consumer.bat --zookeeper localhost:2181 --topic {name of your topic} --from-beginning
