package models.workouts

sealed trait Dimension

object Dimension {
  case object Angle extends Dimension
  case object Distance extends Dimension
  case object Time extends Dimension
  case object Weight extends Dimension
}

case class UnitOfMeasure(
  dimension: Dimension,
  symbol: String
)

object UnitOfMeasure {
  def weight(symbol: String) = UnitOfMeasure(Dimension.Weight, symbol)
  val Pound = weight("lbs")
  val Kilogram = weight("kg")
  val Pood = weight("pood")
  val values = Seq(Pound, Kilogram, Pood)
}

case class Measurement(
  unitOfMeasure: UnitOfMeasure,
  value: BigDecimal
)

case class Variable(
  name: String,
  dimension: Option[Dimension],
  measurement: Option[Measurement]
) {

  require(
    (dimension.isDefined && measurement.isEmpty) ||
      (dimension.isEmpty && measurement.isDefined),
    "exactly one of dimension and measurement must be defined"
  )

}
