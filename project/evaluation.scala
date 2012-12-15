import sbt._
import complete.Parser
import complete.DefaultParsers._

trait Evaluate { self: Build =>
	abstract class Axis {
	  def name: String
	  def configPath: String
	  def points: List[Any]
	  def value(s: String): Any
	  override def toString = name
	}
	case class IntAxis(val name: String, val configPath: String, val points: List[Integer]) extends Axis {
	  def value(s: String) = s.toInt
	}
	case class DoubleAxis(val name: String, val configPath: String, val points: List[Double]) extends Axis {
	  def value(s: String) = s.toDouble
	}
	case class StringAxis(val name: String, val configPath: String, val points: List[String]) extends Axis {
	  def value(s: String) = s
	}
	case class BooleanAxis(val name: String, val configPath: String) extends Axis {
    val points = List(true, false)
	  def value(s: String) = s(0) match { case 't' => true; case 'f' => false }
	}

	type Scenario = Map[Axis, Any]

	object Scenario {
	  type Scenario = Map[Axis,Any]
	
	  def enumerate(axes: List[Axis]): List[Scenario] = axes match {
	    case axis :: List() => axis.points map { p => Map((axis, p)) }
	    case axis :: rest => {
	      val resultOfRest = enumerate(rest)
	      axis.points map { p =>
	        resultOfRest map { c: Scenario => c + (axis -> p) }
	      } flatten
	    }
	    case List() => throw new Exception("should not get here")
	  }
	  def enumerate(axes: Axis*): List[Scenario] = enumerate(axes.toList)
	}
	
	def axes : List[Axis] // to be defined by the build that includes it
	
	// Typesafe conf stuff
	
  implicit def toStringForConfiguration(c: Scenario) = new {
    override def toString: String = c map { case (name, value) => name + " = " + value } mkString "; "
  }
	
	// Autocomplete stuff
	
  val parser = (state: State) =>
    (((axes map { ' ' ~> axisParser(_) } reduce { _ | _ } *) map { _ toMap }): Parser[Scenario])
		
	def axisParser(x: Axis): Parser[(Axis, Any)] =
	  token(x.name <~ '=') ~>
	    token(StringBasic.examples(x.points map { a => a.toString } toSet)) map { v => (x, x.value(v)) }	
}