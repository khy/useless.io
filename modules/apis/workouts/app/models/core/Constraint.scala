package models.workouts.core

import play.api.libs.json.Json

case class Constraint(
  variable: String,
  value: ConstraintAst
)

object Constraint {
  implicit val jsonFormat = Json.format[Constraint]
}
