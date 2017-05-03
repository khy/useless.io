package models.workouts.newy.core

import java.util.UUID
import play.api.libs.json.Json

case class Workout(
  name: Option[String],
  score: Option[ScoreExpression],
  variables: Option[Seq[FreeVariable]],
  task: AbstractTask
)

object Workout {

  implicit val jsonFormat = Json.format[Workout]

}
