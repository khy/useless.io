package models.workouts.newy

import java.util.UUID
import java.time.ZonedDateTime

import models.workouts.newy.core.{BoundVariable, ConcreteTask}

case class Performance(
  guid: UUID,
  workoutGuid: UUID,
  variables: Option[Seq[BoundVariable]],
  tasks: Seq[ConcreteTask],
  createdAt: ZonedDateTime,
  createdByAccount: UUID,
  deletedAt: Option[ZonedDateTime],
  deletedByAccount: Option[UUID]
)
