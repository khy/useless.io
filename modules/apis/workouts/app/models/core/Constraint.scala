package models.workouts.core

import play.api.libs.json.Json

import dsl.workouts.ConstraintExpression

case class Constraint(
  variable: String,
  value: ConstraintExpression
)

object Constraint {
  implicit val jsonFormat = Json.format[Constraint]
}
