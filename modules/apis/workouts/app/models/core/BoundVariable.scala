package models.workouts.core

import play.api.libs.json.Json

case class BoundVariable(
  name: String,
  measurement: MeasurementAst
)

object BoundVariable {
  implicit val jsonFormat = Json.format[BoundVariable]
}
