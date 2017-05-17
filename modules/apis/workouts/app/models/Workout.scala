package models.workouts

import java.util.UUID
import java.time.ZonedDateTime
import play.api.libs.json.Json

import models.workouts.core.{AbstractTask, FreeVariable, ScoreAst}

case class Workout(
  guid: UUID,
  name: Option[String],
  score: Option[ScoreAst],
  variables: Option[Seq[FreeVariable]],
  task: AbstractTask,
  createdAt: ZonedDateTime,
  createdByAccount: UUID,
  deletedAt: Option[ZonedDateTime],
  deletedByAccount: Option[UUID]
)

object Workout {

  implicit val jsonFormat = Json.format[Workout]

}
