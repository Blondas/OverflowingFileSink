import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future
import scala.concurrent.duration._

object Test extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  var cnt = 0
  val xml = "dupa"

  val source: Source[ByteString, NotUsed] =
    Source
      .repeat(xml)
      .map(e => {cnt +=1; e+cnt})
      .take(10)
//      .throttle(10, 1 second, 1, ThrottleMode.shaping)
      .map(ByteString(_))

  val sink = Sink.foreach(println)
  val fileSink: Sink[ByteString, Future[Done]] = MySink.streamToFiles()

  source to fileSink
}