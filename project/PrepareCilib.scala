import java.io.File
import sbt._

object PrepareCilib {
  def go(baseDirectory: File) {
    val cilibXmlDir = baseDirectory / "benchmarks" / "cilib" / "simulator" / "xml"
    val walaXmlDir = cilibXmlDir / "wala"

    //    (cilibXmlDir * "*.xml").get foreach { file =>
    val file = (cilibXmlDir * "*.xml").get head
    
    println("Processing: " + file)
    val xml = scala.xml.XML.loadFile(file)

    val res = (xml \ "simulations" \ "simulation") map { simulation =>
    	simulation
    }

    println(res)


    walaXmlDir.mkdirs
  }
}