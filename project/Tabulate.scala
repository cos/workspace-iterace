import java.io.FileWriter
import dispatch.classic.json.Js
import sjson.json._
import DefaultProtocol._
import JsonSerialization._
import scala.io._
import scala.sys.process._
import java.io.File

object helpful {
  import scala.math.pow
  import Organize._

  def find(pd: Iterable[R], s: S) = pd.find {
    case R(ss, _, _) if ss == s => true
    case _ => false
  }
  def find(pd: Iterable[R], f: S => Boolean) = pd.filter {
    case R(s, _, _) if f(s) => true
    case _ => false
  }

  def ga(l: Iterable[Double]) = pow(
    l.map(v => if (v < 0.5) 0.5 else v).product,
    1 / (l.size.toDouble))

  def hline = "\\finerule"
  def textbf(s: String) = "\\textbf{" + s + "}"

  def nl = "\n"
}

object Tabulate {
  def apply(resultsDir: File, apps: List[String]) = new Tabulate(resultsDir, apps)
}

class Tabulate(resultsDir: File, apps: List[String]) {
  import scala.math._
  import Organize._
  import S._
  import helpful._

  private val organize = Organize(resultsDir, apps)

  private val data = organize.readData(resultsDir)

  private def formatTimeInt(t: Int) = t / 1000 + "." + t % 1000 / 100
  private def formatTimeIntPrecise(t: Int) = t / 1000 + "." + t % 1000 / 10

  private def formatRaceInt(r: Int) = {
    val k = 1000
    val m = k * k

    def f(o: Int, r: Int, let: String) = (if (r * 1.0 / o >= 10) (r / o).toString else "%.1f".format(r * 1.0 / o)) + let

    (r match {
      case r if r > m => f(m, r, "M") // + " ~ " + r
      case r if r > 10 * k => f(k, r, "K") // + " ~ " + r
      case _ => r
    }).toString
  }

  // max has type subject: String -> theMax: Int
  private def stuff(f: R => Int, format: Int => String, max: String => Int, meanFunction: Option[Iterable[Double] => Double] = None): String = {
    var count = 0

    superset filter { _(d) } map (s => {
      val allSujects = apps map { app => find(data(app), s) map { rr => f(rr) } }
      val mean = meanFunction map { _(apps map { app => find(data(app), s) map { rr => f(rr) } getOrElse max(app) } map { _.toDouble }).toInt }
      count += 1
      s.dotStyle + // subject
        " & " + (allSujects map { _ map { format(_) } } map { _ getOrElse "-" } mkString " & ") + // values
        (mean map { " & " + format(_) } getOrElse "") + " \\\\ \n"
    }) grouped (4) map { _.mkString } mkString (hline + nl)
  }

  private def maxOfRaces(app: String) = (data(app) map { _.races }).max

  private def header(prefix: Seq[String] = Seq(), suffix: Seq[String] = Seq()) = {
    (prefix ++ apps ++ suffix).mkString(" & ") + "\\\\\\midrule" + nl
  }

  def showData {
    for ((projectName, projectData) <- data) {
      println(projectName + ":" + projectData.size)

      S.superset foreach (s => {
        val r = find(projectData, s)
        println(s + " - " + (r getOrElse ""))
      })
    }
  }

  def races = header(prefix = Seq("T","F","B","S")) + stuff(r => { r.races }, formatRaceInt, maxOfRaces, None)
  
  // there was an issue with the nesting of time logging and deep-synchronized-time was content twice - this fixes the
  // result errors stemming from that issue
  // (not used anymore below)
  def timeFix(r: R) = r.times.get("deep-synchronized-time").orElse(r.times.get("app-level-synchronized-time")).getOrElse(0)

  def times = header(prefix = Seq("T","F","B","S"), suffix = Seq("avg.")) + stuff(r => { r.time }, formatTimeInt, app => { 600000 }, Some(ga))
  
  def timesDrillDown = { 
    val resultsForAllActive = apps flatMap { app => find(data(app), S(Set(t, f, b, s))) }
    
    val lines:Seq[String] = Seq("pointer-analysis-time","potential-races-time","locksets-time","app-level-synchronized-time","bubble-up-time") map {
        lookForTime =>
          lookForTime.replaceAll("-"," ") + " & " + 
            (resultsForAllActive map { r => formatTimeInt(if(lookForTime=="locksets-time") r.times("app-level-synchronized-time") else 0) } mkString " & ")
        } 
      
    val linesWithTotal:Seq[String] = lines ++ Seq("Total & " + (resultsForAllActive map { r => formatTimeInt(r.time) } mkString " & "))
      
    val tex = header(prefix = Seq("Stage")) + (linesWithTotal mkString "  \\\\\n")
    println(tex)
    tex
  }

  def racesByFeature(f: String) = header(Seq("T","F","B","S") diff Seq(f.head.toUpper.toString)) + stuffByFeature(feature(f.head), r => { r.races }, formatRaceInt, maxOfRaces)

  def timesByFeature(f: String) = header() + stuffByFeature(feature(f.head), r => { r.time }, formatTimeInt, app => { 600000 })

  def synchronizationLevelEffect = header() + effectOfSynchronizationLevel(r => { r.races }, formatRaceInt, maxOfRaces)
  
  private def formatRatio(before: Float, after:Float) = 
    if (after > 0)
                "%.2f".format(before * 1.0 / after)
              else
                if(before > 0)
                	"$\\infty$"
                else 
                	"NaN"

  private def stuffByFeature(feature: String, func: R => Int, format: Int => String, max: String => Int) = {
    var count = 0
    
    

    // !!! reversed - positive is native and the other way around
    (negative(Set("deep-synchronized","synchronized")).zip(positive(Set("deep-synchronized","synchronized"))) map {
      case (sp, sn) => {
        "" + dotStyle(sp.sl filter { f => f.toLower != feature.head.toLower && f != 'd' } mkString) + " & " +
          (apps map { subject =>
            val rp = find(data(subject), sp)
            val rn = find(data(subject), sn)

            if (rp.isDefined && rn.isDefined) {
              val before = func(rp.get)
              val after = func(rn.get)
              //              ((before - after) * 100.0 / before) toInt
              formatRatio(before, after)
            } else
              "-"

            //            def normalize(r: Option[R]) = r map (r => func(r).toDouble) getOrElse max(app).toDouble
            //            (normalize(rp), normalize(rn))
          } mkString " & ") + " \\\\"
        //        val (pos, neg) = allNormalizedForScenario unzip

        //        val themean = (ga(pos) - ga(neg)).toInt
        //        print(" & " + format(themean))
        //        (pos, neg, themean.toDouble)
      }
    } mkString "\n") + nl

    //    val (pos, neg, theOtherMeans) = bigResults.unzip3
    //    val fullTranspose = (pos.transpose zip neg.transpose)
    //    val means = fullTranspose map {
    //      case (pos, neg) => {
    //        val themean = (ga(pos) - ga(neg)).toInt
    //        print(" & "); textbf(format(themean))
    //        themean.toDouble
    //      }
    //    }
    //    val theothermean = ga(theOtherMeans).toInt
    //    println(" & "); textbf(ga(means).toInt + " | " + format(theothermean))
    //    println(" \\\\")
  }

  private def effectOfSynchronizationLevel(func: R => Int, format: Int => String, max: String => Int) = {
    var count = 0

    val baseline = negative(S.d) intersect negative(S.s)
    val sOnly = positive(S.d) intersect negative(S.s)
    val aOnly = negative(S.d) intersect positive(S.s)
    val both = positive(S.d) intersect positive(S.s)
    
    val output = new StringBuffer

    // !!! reversed - positive is native and the other way around
    val bigResults = baseline zip sOnly zip aOnly zip both map {
      case (((base, s), a), both) => {
        output.append("\\texttt{" + base + "-" + s + "/" + a + "/" + both + "}\n")
        val allNormalizedForScenario = for (app <- apps) yield {
          val rbase = find(data(app), base)
          val rs = find(data(app), s)
          val ra = find(data(app), a)
          val rboth = find(data(app), both)

          val strs = if (rbase.isDefined && rs.isDefined)
            formatRatio(func(rbase.get), func(rs.get))
          else
            "-"

          val stra = if (rbase.isDefined && ra.isDefined)
            formatRatio(func(rbase.get) , func(ra.get))
          else
            "-"

          val strboth = if (rbase.isDefined && rboth.isDefined)
            formatRatio(func(rbase.get) , func(rboth.get))
          else
            "-"

          output.append(" & " + strs + "/" + stra + "/" + strboth)
        }
        output.append(" \\\\\n")
      }
    }
    output.toString.replace("1.00/1.00/1.00", "1.0").replace("NaN/NaN/NaN", "NaN")
  }

  private def times(app: String) = data(app) map { _.time }

  private def positive(feature: String) = superset.filter {
    case s: S if s(feature) => true
    case _ => false
  }
  
  private def positive(features: Set[String]) = superset.filter {
    case s: S if !features.exists(!s(_)) => true
    case _ => false
  }

  private def negative(feature: String) = superset diff positive(feature)
  private def negative(features: Set[String]) = superset diff positive(features)  

  import scala.math._
  def differences {
    //  val feature = args.head

    for ((projectName, projectData) <- data) {
      println(projectName + ":" + projectData.size)
      //    val ds = positive(projectData, t) map (rp => {
      //      val rn = negative(projectData, t).find {case R(s, _,_ ) if s == rp.s - t => true; case _ => false}
      //      val d = rp.totalTime - rn.get.totalTime
      //      println(d)
      //      d
      //    })
      println(projectData)
      val pvals = positive(projectData, S.t) map { _.races } map (_.toDouble + 0.9)
      val nvals = negative(projectData, S.t) map { _.races } map (_.toDouble + 0.9)
      println(pvals)
      println(pow(pvals.product, 1 / pvals.size.toDouble) - pow(nvals.product, 1 / nvals.size.toDouble))
    }

    def positive(results: List[R], feature: String) = results.filter {
      case R(s, _, _) if s(feature) => true
      case _ => false
    }

    def negative(results: List[R], feature: String) = results.filter {
      case R(s, _, _) if !s(feature) => true
      case _ => false
    }
  }
}