package models.workouts.newy.core

import play.api.libs.json.Json

case class BoundVariable(
  name: String,
  measurement: MeasurementExpression
)

object BoundVariable {
  implicit val jsonFormat = Json.format[BoundVariable]
}
