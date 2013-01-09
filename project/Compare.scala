
import dispatch.json.Js
import java.io.File
import sjson.json._
import DefaultProtocol._
import JsonSerialization._

object Compare {
  def compare(iteRaceFile: File, jChordFile: File) {
    println(iteRaceFile)
    val iterace = parse(iteRaceFile)
    val coref = parse(jChordFile)

//    coref collect { r: Map[String, String] =>
    	
//    }
  }
  def parse(f: File) = {
    val json = Js(scala.io.Source.fromFile(f).mkString)
    fromjson[List[Map[String, String]]](json)
  }
}