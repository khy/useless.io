package models.workouts.newy.core

import play.api.libs.json.Json

case class FreeVariable(
  name: String,
  dimension: Dimension
)

object FreeVariable {
  implicit val jsonFormat = Json.format[FreeVariable]
}
