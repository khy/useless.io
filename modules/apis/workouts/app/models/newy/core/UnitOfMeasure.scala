package models.workouts.newy.core

case class UnitOfMeasure(dimension: Dimension, symbol: String)

object UnitOfMeasure {
  val Degree = UnitOfMeasure(Dimension.Angle, "deg")

  val Meter = UnitOfMeasure(Dimension.Distance, "m")
  val Foot = UnitOfMeasure(Dimension.Distance, "ft")
  val Inch = UnitOfMeasure(Dimension.Distance, "in")

  val Second = UnitOfMeasure(Dimension.Time, "sec")
  val Minute = UnitOfMeasure(Dimension.Time, "min")

  val Pound = UnitOfMeasure(Dimension.Weight, "lb")
  val Kilogram = UnitOfMeasure(Dimension.Weight, "kg")
  val Pood = UnitOfMeasure(Dimension.Weight, "pood")

  val values = Seq(Degree, Meter, Foot, Inch, Second, Minute, Pound, Kilogram, Pood)

  assert(
    values.map(_.symbol).distinct == values.map(_.symbol),
    "each UnitOfMeasure symbol must be unique"
  )
}
