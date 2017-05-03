package models.workouts

import java.util.UUID
import java.time.ZonedDateTime
import play.api.libs.json.Json

import models.workouts.core.{AbstractTask, BoundVariable}

case class Variant(
  guid: UUID,
  workoutGuid: UUID,
  name: String,
  task: AbstractTask,
  createdAt: ZonedDateTime,
  createdByAccount: UUID,
  deletedAt: Option[ZonedDateTime],
  deletedByAccount: Option[UUID]
)

object Variant {

  implicit val jsonFormat = Json.format[Variant]

}
