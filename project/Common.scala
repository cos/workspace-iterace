import sbt._
import Keys._

trait Common { self: Build =>
  lazy val commonSettings = Defaults.defaultSettings ++ Seq(
    scalaVersion := "2.10.0")

  lazy val util = Project(id = "Util", settings = commonSettings,
    base = file("Util"))

  lazy val walaFacade = Project(id = "WALAFacade", settings = commonSettings,
    base = file("WALAFacade")) dependsOn (walaUtil, walaShrike, walaCore)

  lazy val walaUtil = Project(id = "walaUtil", settings = commonSettings,
    base = file("wala/com.ibm.wala.util"))

  lazy val walaShrike = Project(id = "walaShrike", settings = commonSettings,
    base = file("wala/com.ibm.wala.shrike")).dependsOn(walaUtil)

  lazy val walaCore = Project(id = "walaCore", settings = commonSettings,
    base = file("wala/com.ibm.wala.core")).dependsOn(walaUtil, walaShrike)
}