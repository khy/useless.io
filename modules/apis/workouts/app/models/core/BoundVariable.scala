package models.workouts.core

import play.api.libs.json.Json

import dsl.workouts.MeasurementExpression

case class BoundVariable(
  name: String,
  measurement: MeasurementExpression
)

object BoundVariable {
  implicit val jsonFormat = Json.format[BoundVariable]
}
