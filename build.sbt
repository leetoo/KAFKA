name := "Kafka, Akka HTTP and Akka Streams"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= {
  val akka = "com.typesafe.akka"
  Seq(
    akka                        %% "akka-actor"                           % "2.4.10",
    akka                        %% "akka-slf4j"                           % "2.4.10",
    akka                        %% "akka-stream-kafka"                    % "0.12"
  )
}