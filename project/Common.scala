import sbt._
import Keys._

trait Common { self: Build =>
  lazy val util = Project(id = "Util",
    base = file("Util"))

  lazy val walaFacade = Project(id = "WALAFacade",
    base = file("WALAFacade")) dependsOn (util, walaUtil, walaShrike, walaCore)

  lazy val walaUtil = Project(id = "walaUtil",
    base = file("wala/com.ibm.wala.util"))

  lazy val walaShrike = Project(id = "walaShrike",
    base = file("wala/com.ibm.wala.shrike")).dependsOn(walaUtil)

  lazy val walaCore = Project(id = "walaCore",
    base = file("wala/com.ibm.wala.core")).dependsOn(walaUtil, walaShrike)
}