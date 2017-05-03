package models.workouts.newy.core

import play.api.libs.json.Json

case class Constraint(
  variable: String,
  value: ConstraintExpression
)

object Constraint {
  implicit val jsonFormat = Json.format[Constraint]
}
