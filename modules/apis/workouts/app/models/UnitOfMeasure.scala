package models.workouts

case class UnitOfMeasure(
  dimension: Dimension,
  symbol: String
)

object UnitOfMeasure {
  def weight(symbol: String) = UnitOfMeasure(Dimension.Weight, symbol)
  val Pound = weight("lbs")
  val Kilogram = weight("kg")
  val Pood = weight("pood")
}
