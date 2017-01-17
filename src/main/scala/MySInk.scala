import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import akka.Done
import akka.stream.scaladsl.Sink
import akka.util.ByteString

import scala.concurrent.Future

object MySink {
  import Sink.foreach

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

    foreach[ByteString]{ e =>
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
