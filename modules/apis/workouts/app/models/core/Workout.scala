package models.workouts.core

import java.util.UUID
import play.api.libs.json.Json

import dsl.workouts.compile.Ast

case class Workout(
  name: Option[String],
  score: Option[Code[Ast.Arithmetic]],
  variables: Option[Seq[FreeVariable]],
  task: AbstractTask
)

object Workout {

  implicit val jsonFormat = Json.format[Workout]

}
