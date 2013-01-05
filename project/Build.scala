import sbt._
import Keys._
import Build.data
import java.io.File

object WorkspaceBuild extends Build with Common with Evaluate {
  lazy val workspace = Project(id = "workspace",
    base = file("."),
    settings = Project.defaultSettings ++
      Seq(benchTask, benchAllTask, resultsDirectorySetting, mergeForTask, mergeAllTask,
        showDataTask, tabulateRacesTask, tabulateTimesTask, tabulateFeatureRacesTask, racesTask))
    .aggregate(iteRace)

  lazy val iteRace = Project(id = "IteRace",
    base = file("IteRace"),
    settings = Project.defaultSettings ++
      Seq(benchRunnerTask,
        baseDirectory in benchRunner := new File("."),
        unmanagedResourceDirectories in Compile += new File("project/subjects").getAbsoluteFile))
    .dependsOn(
      util,
      walaFacade,
      walaUtil,
      walaShrike,
      walaCore,
      parallelArrayMock)

  lazy val parallelArrayMock = Project(id = "ParallelArray-mock", base = file("lib/parallelArray.mock"))

  val subjectAxis = StringAxis("subject", "subject",
    List("em3d", "bh", "mc", "junit", "coref", "lucene", "weka"))

  case class IteRaceOptionAxis(override val name: String) extends BooleanAxis(name, "iterace.options." + name)

  def axes = List(subjectAxis) ++ (
    List("two-threads", "filtering", "bubble-up", "deep-synchronized", "synchronized") map { axisName =>
      IteRaceOptionAxis(axisName)
    })

  object Keys {
    val bench = InputKey[Unit]("bench")
    val races = InputKey[Unit]("races")
    val benchAll = InputKey[Unit]("bench-all")
    val resultsDirectory = SettingKey[File]("results-directory")
    val mergeFor = InputKey[Unit]("merge-for") // merges all scenarios for a certain subject in a single <subject>.json
    val mergeAll = TaskKey[Unit]("merge-all") // merges all <subject>.json into all.json
    val showData = TaskKey[Unit]("show-data")
    val tabulateRaces = TaskKey[Unit]("tabulate-races")
    val tabulateTimes = TaskKey[Unit]("tabulate-times")
    val tabulateFeatureRaces = InputKey[Unit]("tabulate-feature-races")
  }

  lazy val resultsDirectorySetting = Keys.resultsDirectory <<= target(_ / "results")

  lazy val mergeAllTask = Keys.mergeAll <<= (Keys.resultsDirectory) map { resultsDir =>
    val o = Organize(resultsDir, subjectAxis.points)
    subjectAxis.points foreach { o.mergeJSonForSubject(_) }
    o.mergeAll
  }
  lazy val showDataTask = Keys.showData <<= (Keys.resultsDirectory) map { Tabulate(_, subjectAxis.points).showData }
  lazy val tabulateRacesTask = Keys.tabulateRaces <<= (Keys.resultsDirectory) map { resultsDir =>
    IO.write(resultsDir / "races.tex", Tabulate(resultsDir, subjectAxis.points).races)
  }
  lazy val tabulateTimesTask = Keys.tabulateTimes <<= (Keys.resultsDirectory) map { resultsDir =>
    IO.write(resultsDir / "times.tex", Tabulate(resultsDir, subjectAxis.points).times)
  }
  lazy val tabulateFeatureRacesTask = Keys.tabulateFeatureRaces <<= inputTask { (argTask: TaskKey[Seq[String]]) =>
    (argTask, Keys.resultsDirectory) map { (args, resultsDir) =>
      args foreach { arg =>
        IO.write(resultsDir / ("races-by-" + arg + ".tex"), Tabulate(resultsDir, subjectAxis.points).racesByFeature(arg))
      }
    }
  }

  lazy val benchTask = Keys.bench <<= InputTask(parser)(benchDef)
  lazy val benchDef = { parsed: TaskKey[Scenario] =>
    {
      (fullClasspath in iteRace in Compile, mainClass in iteRace, benchRunner in iteRace, streams, parsed, Keys.resultsDirectory) map {
        bench(_, _, _, _, _, _)
      }
    }
  }

  lazy val racesTask = Keys.races <<= InputTask(parser)(racesDef)
  lazy val racesDef = { parsed: TaskKey[Scenario] =>
    {
      (fullClasspath in iteRace in Compile, mainClass in iteRace, benchRunner in iteRace, streams, parsed, Keys.resultsDirectory) map {
        bench(_, _, _, _, _, _, true)
      }
    }
  }

  lazy val benchAllTask = Keys.benchAll <<= InputTask(parser)(benchAllDef)
  lazy val benchAllDef = (parsed: TaskKey[Scenario]) => {
    (fullClasspath in iteRace in Compile, mainClass in iteRace, benchRunner in iteRace, streams, parsed, Keys.resultsDirectory) map { (cp, mc, r, streams, scenario, resultsDir) =>
      Scenario.enumerate(axes) filter { s => s.matches(scenario) } foreach { s =>
        bench(cp, mc, r, streams, s, resultsDir)
      }
    }
  }

  lazy val mergeForTask = Keys.mergeFor <<= inputTask { (argTask: TaskKey[Seq[String]]) =>
    (argTask, Keys.resultsDirectory) map { (args: Seq[String], resultsDirectory) =>
      args foreach { Organize(resultsDirectory, subjectAxis.points).mergeJSonForSubject(_) }
    }
  }

  def bench(cp: Seq[Attributed[File]], mc: Option[String], r: ScalaRun, streams: TaskStreams, scenario: Scenario, resultsDirectory: File, genRaces: Boolean = false) = {
    val subject = scenario(subjectAxis).asInstanceOf[String]
    val scenarioIteRaceOptions = scenario filter { case (_: IteRaceOptionAxis, _) => true; case _ => false }
    val options = (scenarioIteRaceOptions map { case (k, v) => k.configPath + "=" + v }).toSeq
    val s = Organize.S(scenarioIteRaceOptions collect { case (IteRaceOptionAxis(name), true) => name } toSet)
    val logDir = resultsDirectory / subject
    logDir.mkdirs
    val logFile = logDir / ((s.sl map { k => k + (if (k >= 'a') "-" else "_") } mkString) + ".json")
    val racesFile = logDir / ((s.sl map { k => k + (if (k >= 'a') "-" else "_") } mkString) + ".races")

    println(racesFile.getAbsolutePath)

    val fullOptions = options ++ Seq(
      "config=" + subject,
      "iterace.log-file=" + logFile,
      "iterace.timeout=600000") ++ (if (genRaces) Seq("iterace.races-file=" + racesFile) else Seq())

    streams.log.info(options.toString)
    r.run(mc.get, data(cp), fullOptions, streams.log) foreach { streams.log.warn(_) }
    if (genRaces)
      println("\n" + IO.read(racesFile))
  }
}