import java.io.InputStream
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorAttributes, ActorMaterializer, IOResult, ThrottleMode}
import akka.stream.scaladsl.{FileIO, Flow, Sink, Source, StreamConverters}
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
  val fileSink: Sink[ByteString, Future[Done]] = streamToFiles()

  source to fileSink


  def streamToFiles(sizeLimit: Int = 10, elemLimit: Int = 10, fileBaseName: String = "someFile"): Sink[ByteString, Future[Done]] = {
    import StandardOpenOption._

    var sizeCnt, elemCnt, fileCnt: Int = 0

    def newPath: Path = {
      fileCnt += 1

      if (fileCnt -1 > 0) Paths.get(fileBaseName + fileCnt)
      else Paths.get(fileBaseName)
    }

    var currPath: Path = newPath

    println(s"= create new file: $currPath")

    Sink.foreach[ByteString]{ e =>
      val eSize = e.length
      val cntsZeroed = sizeCnt == 0 && elemCnt == 0
      val underLimits = ( sizeCnt + eSize <= sizeLimit ) && ( elemCnt + 1 <= elemLimit)

      if (underLimits) {
        sizeCnt += eSize; elemCnt += 1

        println("write to file")
        Files.write(currPath, e.toArray, CREATE, WRITE, APPEND)
      } else {
        sizeCnt = eSize; elemCnt = 1

        currPath = newPath
        println(s"= create new file: $currPath")
        println("write to file")
        Files.write(currPath, e.toArray, CREATE, WRITE, APPEND)
      }
    }
  }
}
