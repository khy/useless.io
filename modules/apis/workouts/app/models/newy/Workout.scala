package models.workouts.newy

import java.util.UUID
import java.time.ZonedDateTime
import play.api.libs.json.Json

import models.workouts.newy.core.{AbstractTask, FreeVariable, ScoreExpression}

case class Workout(
  guid: UUID,
  name: Option[String],
  score: Option[ScoreExpression],
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
