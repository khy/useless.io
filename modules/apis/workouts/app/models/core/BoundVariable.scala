package models.workouts.core

import play.api.libs.json.Json

import dsl.workouts.compile.Ast

case class BoundVariable(
  name: String,
  measurement: Code[Ast.Measurement]
)

object BoundVariable {
  implicit val jsonFormat = Json.format[BoundVariable]
}
