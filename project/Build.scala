import sbt._
import Keys._
import Build.data
import java.io.File

object WorkspaceBuild extends Build with Common with Evaluate {

  val dependencyOnIteRace = libraryDependencies ++= Seq(
    "University of Illinois" %% "iterace" % "0.5")
    
  val iteRaceMainClass = mainClass := Some("iterace.IteRace")
  
  val mavenResolver = resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
  
  val addSubjects = unmanagedResourceDirectories in Compile += new File("project/subjects").getAbsoluteFile
  
  val baseDirectoryForBenchRunner = baseDirectory in benchRunner := new File(".")

  lazy val workspace = Project(id = "workspace",
    base = file("."),
    settings = Project.defaultSettings ++ dependencyOnIteRace ++
      Seq(benchTask, benchAllTask, resultsDirectorySetting, mergeForTask, mergeAllTask,
        showDataTask, tabulateRacesTask, tabulateTimesTask, tabulateFeatureRacesTask,
        racesTask, compareTask, tabulateSyncComparisonTask, tabulateAllTask, prepareCilibTask, iteRaceMainClass, 
        benchRunnerTask, baseDirectoryForBenchRunner, mavenResolver, addSubjects))

  //  lazy val iteRace = Project(id = "IteRace",
  //    base = file("IteRace"),
  //    settings = Project.defaultSettings ++
  //      Seq(scalaVersionSetting, benchRunnerTask, 
//          baseDirectory in benchRunner := new File(".")))//,
  //        unmanagedResourceDirectories in Compile += new File("project/subjects").getAbsoluteFile)) // comment this line to sbt-eclipse
  //    .dependsOn(
  //      util,
  //      walaFacade,
  //      walaUtil,
  //      walaShrike,
  //      walaCore,
  //      parallelArrayMock)

  lazy val scalaVersionSetting = scalaVersion := "2.10"

  //  lazy val parallelArrayMock = Project(id = "ParallelArray-mock", base = file("lib/parallelArray.mock"))

  val subjectAxis = StringAxis("subject", "subject",
    List("em3d", "mc", "junit", "coref", "lucene", "weka", "cilib")) // "bh"

  object IteRaceOptionAxis {
    def apply(name: String) = BooleanAxis(name, "iterace.options." + name)
    def unapply(b: BooleanAxis) = b match {
      case BooleanAxis(name, longName) if longName.startsWith("iterace.options.") => Some(name)
      case _ => None
    }
  }

  lazy val techniques = List("two-threads", "filtering", "bubble-up", "deep-synchronized", "synchronized")

  def axes = List(subjectAxis) ++ (
    techniques map { axisName =>
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
    val tabulateSyncComparison = TaskKey[Unit]("tabulate-sync-comparison")
    val compare = InputKey[Unit]("compare-for")
    val tabulateAll = TaskKey[Unit]("tabulate-all")
    val prepareCilib = TaskKey[Unit]("prepare-cilib")
  }

  lazy val resultsDirectorySetting = Keys.resultsDirectory <<= target(_ / "results")

  lazy val mergeAllTask = Keys.mergeAll <<= (Keys.resultsDirectory) map { resultsDir =>
    val o = Organize(resultsDir, subjectAxis.points)
    subjectAxis.points foreach { o.mergeJSonForSubject(_) }
    o.mergeAll
  }

  lazy val tabulateAllTask = Keys.tabulateAll <<= (Keys.mergeAll, Keys.tabulateRaces, Keys.tabulateTimes, Keys.resultsDirectory) map {
    case (_, _, _, resultsDir) =>
      techniques foreach { arg =>
        IO.write(resultsDir / ("races-by-" + arg + ".tex"), Tabulate(resultsDir, subjectAxis.points).racesByFeature(arg))
      }
  }

  lazy val showDataTask = Keys.showData <<= (Keys.resultsDirectory) map { Tabulate(_, subjectAxis.points).showData }

  lazy val prepareCilibTask = Keys.prepareCilib <<= (baseDirectory) map { baseDirectory =>
    PrepareCilib.go(baseDirectory)
  }

  lazy val tabulateRacesTask = Keys.tabulateRaces <<= (Keys.resultsDirectory) map { resultsDir =>
    IO.write(resultsDir / "races.tex", Tabulate(resultsDir, subjectAxis.points).races)
  }
  lazy val tabulateTimesTask = Keys.tabulateTimes <<= (Keys.resultsDirectory) map { resultsDir =>
    IO.write(resultsDir / "times.tex", Tabulate(resultsDir, subjectAxis.points).times)
  }
  lazy val tabulateSyncComparisonTask = Keys.tabulateSyncComparison <<= (Keys.resultsDirectory) map { resultsDir =>
    IO.write(resultsDir / "sync-comparison.tex", Tabulate(resultsDir, subjectAxis.points).synchronizationLevelEffect)
  }

  lazy val tabulateFeatureRacesTask = Keys.tabulateFeatureRaces <<= inputTask { (argTask: TaskKey[Seq[String]]) =>
    (argTask, Keys.resultsDirectory) map { (args, resultsDir) =>
      args foreach { arg =>
        IO.write(resultsDir / ("races-by-" + arg + ".tex"), Tabulate(resultsDir, subjectAxis.points).racesByFeature(arg))
      }
    }
  }

  lazy val compareTask = Keys.compare <<= inputTask { (argTask: TaskKey[Seq[String]]) =>
    (argTask, baseDirectory) map { (args, base) =>
      args foreach { arg =>
        val resultsDir = base / "results"
        resultsDir.mkdirs
        val iteRaceFile = resultsDir / (arg + "-iterace.json")
        val jChordFile = resultsDir / (arg + "-jchord.json")

        Compare.compare(iteRaceFile, jChordFile)
      }
    }
  }

  lazy val benchTask = Keys.bench <<= InputTask(parser)(benchDef)
  lazy val benchDef = { parsed: TaskKey[Scenario] =>
    {
      (fullClasspath in Compile, mainClass, benchRunner, streams, parsed, Keys.resultsDirectory) map {
        bench(_, _, _, _, _, _)
      }
    }
  }

  lazy val racesTask = Keys.races <<= InputTask(parser)(racesDef)
  lazy val racesDef = { parsed: TaskKey[Scenario] =>
    {
      (fullClasspath in Compile,
        mainClass,
        benchRunner,
        streams,
        parsed,
        Keys.resultsDirectory) map {
          bench(_, _, _, _, _, _, true)
        }
    }
  }

  lazy val benchAllTask = Keys.benchAll <<= InputTask(parser)(benchAllDef)
  lazy val benchAllDef = (parsed: TaskKey[Scenario]) => {
    (fullClasspath in Compile, mainClass, benchRunner, streams, parsed, Keys.resultsDirectory) map { (cp, mc, r, streams, scenario, resultsDir) =>
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
    val scenarioIteRaceOptions = scenario filter { case (IteRaceOptionAxis(_), _) => true; case _ => false }
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
      "iterace.timeout=1200000") ++ (if (genRaces) Seq("iterace.races-file=" + racesFile) else Seq())

    streams.log.info(options.toString)
    r.run(mc.get, data(cp), fullOptions, streams.log) foreach { streams.log.warn(_) }
    if (genRaces)
      println("\n" + IO.read(racesFile))
  }
}