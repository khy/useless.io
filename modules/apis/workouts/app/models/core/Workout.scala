package models.workouts.core

import java.util.UUID
import play.api.libs.json.Json

case class Workout(
  name: Option[String],
  score: Option[ScoreAst],
  variables: Option[Seq[FreeVariable]],
  task: AbstractTask
)

object Workout {

  implicit val jsonFormat = Json.format[Workout]

}
