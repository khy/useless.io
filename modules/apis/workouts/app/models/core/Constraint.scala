package models.workouts.core

import play.api.libs.json.Json

import dsl.workouts.compile.Ast

case class Constraint(
  variable: String,
  value: Code[Ast.Arithmetic]
)

object Constraint {
  implicit val jsonFormat = Json.format[Constraint]
}
