import sbt._
import Keys._

object WorkspaceBuild extends Build with Common {
  lazy val workspace = Project(id = "workspace",
    base = file(".")) aggregate (iteRace)

  lazy val iteRace = Project(id = "IteRace",
    base = file("IteRace")) dependsOn (
      util,
      walaFacade,
      walaUtil,
      walaShrike,
      walaCore, 
      parallelArrayMock)

  lazy val parallelArrayMock = Project(id = "ParallelArray-mock",
    base = file("lib/parallelArray.mock"))
}