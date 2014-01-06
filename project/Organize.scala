import java.io.FileWriter
import dispatch.classic.json.Js
import sjson.json._
import DefaultProtocol._
import JsonSerialization._
import scala.io._
import scala.sys.process._
import java.io.File
import sbt._

object Organize {
  // result
  case class R(s: S, racess: Map[String, Int], times: Map[String, Int]) {
    val races = racess("races")
    val time = times.values.sum

    override def toString = "R(" + s + ", " + races + ", " + time + ")"
  }


  def dotStyle(short: String) = short map {case c => if (c.isUpper) "\\bl" else ""} mkString "&"
  // scenario
  case class S(features: Set[String]) {

    private val allOnString = List("two-threads", "filtering", "bubble-up", "deep-synchronized", "synchronized")
    private val allOnChar = allOnString map { shorten(_) }

    def short = sl filter { _ != 'd' } mkString
    override def toString = sl.mkString
    val sl = allOnChar map (ss => { if (features.map(shorten(_)).contains(ss)) ss.toUpper else ss })
    
    def dotStyle = Organize.dotStyle(short)

    def apply(feature: String) = features.contains(feature)

    def displayWithout(s: String) = sl.filter(c => !(c == shorten(s)))

    private def shorten(str: String): Char = if (features.contains(str)) str.toUpperCase.head else str.head
  }

  object S {
    val t = "two-threads"
    val f = "filtering"
    val b = "bubble-up"
    val d = "deep-synchronized"
    val s = "synchronized"

    val full = S(Set(t, f, b, d, s))

    val superset = powerset(full.features).map(s => S(s.toSet)).toList.sortBy(_.toString).reverse

    val smallSuperset = superset filter { !_(d) }

    def powerset[T](s: Set[T]): Set[Set[T]] = {
      if (s.tail.isEmpty)
        Set(Set(), s)
      else {
        val other = powerset(s.tail)
        (other) ++ (other map { _ + s.head })
      }
    }

    def feature(feat: Char): String = feat.toLower match {
      case 't' => t
      case 'f' => f
      case 'b' => b
      case 'd' => d
      case 's' => s
    }
  }

  def apply(resultsDir: File, apps: List[String]) = new Organize(resultsDir, apps)
}

class Organize(resultsDir: File, apps: List[String]) {
  import Organize._

  def mergeJSonForSubject(subject: String) {
    val bigJson = new FileWriter(resultsDir / (subject + ".json"))
    val newData: Map[String, Map[String, String]] = ((resultsDir / subject) * "*.json").get map (file => {
      val source = scala.io.Source.fromFile(file)
      val data = source.mkString
      val scenario = file.name.split('.').head.replace("_", "").replace("-", "")
//      try {
        println("Merging "+file)
        val jsondata = Js(data)
        val results = fromjson[Map[String, String]](jsondata)
        (if (scenario == "") "NONE" else scenario, results)
//      } catch {
//        case _  => null
//      }
    }) filter { _ != null } toMap

    println(newData.size)

    val newJson = tojson[Map[String, Map[String, String]]](newData)

    bigJson.write(newJson.toString())
    bigJson.close()
  }

  def mergeAll {
    val newData = apps map { subject => resultsDir / (subject + ".json") } map { file =>
      {
        val source = scala.io.Source.fromFile(file)
        val data = source.mkString
        val subject = file.name.split('.').head
        val jsondata = Js(data)
        val results = fromjson[Map[String, Map[String, String]]](jsondata)
        (subject, results)
      }
    } toMap

    val bigJson = new FileWriter(resultsDir / "all.json")
    bigJson.write(tojson(newData).toString())
    bigJson.close()
  }

  def readData(resultsDir: File) = {
    val source = scala.io.Source.fromFile(resultsDir / "all.json")
    val stringData = source.mkString
    val jsonData = Js(stringData)
    val data = fromjson[Map[String, Map[String, Map[String, String]]]](jsonData)

    for ((projectName, projectData) <- data) yield {
      (projectName,
        (projectData filter { case (_, result) => result.get("timeout") == None } map {
          case (scenario, result) =>
            var r = result filter { case (k, _) => k != "timeout" } mapValues (_.toInt)
            R(S(scenario filter { _.isUpper } map { S.feature(_) } toSet),
              r.filterKeys(!_.contains("time")),
              r.filterKeys(_.contains("time")))
        }) toList)
    }
  }
}
