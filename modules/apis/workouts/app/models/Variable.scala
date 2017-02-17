package models.workouts

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
