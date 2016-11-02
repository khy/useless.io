package models.workouts

case class Variable(
  name: String,
  dimension: Option[Dimension],
  measurment: Option[Measurement]
)
