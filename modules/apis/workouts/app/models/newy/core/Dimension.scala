package models.workouts.newy.core

case class Dimension(key: String)

object Dimension {
  val Angle = Dimension("angle")
  val Distance = Dimension("distance")
  val Time = Dimension("time")
  val Weight = Dimension("weight")

  val values = Seq(Angle, Distance, Time, Weight)
}
