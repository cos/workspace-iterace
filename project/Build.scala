import sbt._
import Keys._
import Build.data

object WorkspaceBuild extends Build with Common with Evaluate {
  lazy val workspace = Project(id = "workspace",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(benchTask))
    .aggregate(iteRace)

  lazy val iteRace = Project(id = "IteRace",
    base = file("IteRace")) dependsOn (
      util,
      walaFacade,
      walaUtil,
      walaShrike,
      walaCore,
      parallelArrayMock)

  lazy val parallelArrayMock = Project(id = "ParallelArray-mock", base = file("lib/parallelArray.mock"))

  val projectAxis = StringAxis("project", "project", List("bh", "coref", "em3d", "junit", "lucene", "mc", "weka"))
  
  def axes = List(projectAxis) ++ (
    List("two-threads", "known-safe-filtering", "bubble-up", "deep-synchronized", "app-level-synchronized") map { axis =>
    BooleanAxis(axis, "iterace." + axis)
  })

  object Keys {
    val bench = InputKey[Unit]("bench")
  }

  lazy val benchTask = Keys.bench <<= InputTask(parser)(benchDef)

  lazy val benchDef = (parsed: TaskKey[Scenario]) => {
    (fullClasspath in (iteRace, Test), mainClass in (iteRace, Test, run), runner in (iteRace, Test, run), streams, parsed) map { (cp, mc, r, s, scenario) => { 
      	val arguments = Seq(scenario(projectAxis).asInstanceOf[String])
      	scenario foreach {println(_)}
      	val options = scenario filter { case (_, v:Boolean) => v; case _ => false } map {case (k, _) => k.toString}
      	s.log.info(options.toString)
      	toError(r.run(mc.get, data(cp), arguments ++ options, s.log))
      }
    }
  }

}