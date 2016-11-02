package models.workouts

sealed trait Dimension

object Dimension {
  case object Angle extends Dimension
  case object Distance extends Dimension
  case object Time extends Dimension
  case object Weight extends Dimension
}
