package models.workouts

case class Dimension(key: String)

object Dimension {
  val Angle = Dimension("angle")
  val Distance = Dimension("distance")
  val Time = Dimension("time")
  val Weight = Dimension("weight")

  val values = Seq(Angle, Distance, Time, Weight)
}

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

case class Measurement(
  unitOfMeasure: UnitOfMeasure,
  value: BigDecimal
)
