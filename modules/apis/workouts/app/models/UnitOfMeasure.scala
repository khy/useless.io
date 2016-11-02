package models.workouts

sealed trait UnitOfMeasure {
  def dimension: Dimension
  def symbol: String
}

object UnitOfMeasure {
  case class Weight(symbol: String) extends UnitOfMeasure {
    val dimension = Dimension.Weight
  }
  val Pound = Weight("lbs")
  val Kilogram = Weight("kg")
  val Pood = Weight("pood")
}
