package models.workouts.newy

import java.util.UUID
import java.time.ZonedDateTime

import models.workouts.newy.core.{AbstractTask, BoundVariable}

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
