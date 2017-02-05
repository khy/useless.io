package models.workouts

import java.util.UUID
import java.time.ZonedDateTime

package core {

  case class Movement(
    name: String,
    variables: Option[Seq[Variable]]
  )

  case class Workout(
    parentGuid: Option[UUID],
    name: Option[String],
    reps: Option[Formula],
    time: Option[Measurement],
    score: Option[String],
    tasks: Option[Seq[SubTask]],
    movement: Option[TaskMovement]
  )

  case class SubTask(
    reps: Option[Formula],
    time: Option[Measurement],
    tasks: Option[Seq[SubTask]],
    movement: Option[TaskMovement]
  )

  case class TaskMovement(
    guid: UUID,
    score: Option[String], // why is score an attribute of movement?
    variables: Option[Seq[Variable]]
  )

}

case class Movement(
  guid: UUID,
  name: String,
  variables: Option[Seq[Variable]],
  createdAt: ZonedDateTime,
  createdByAccount: UUID,
  deletedAt: Option[ZonedDateTime],
  deletedByAccount: Option[UUID]
)

case class Workout(
  guid: UUID,
  parentGuid: Option[UUID],
  name: Option[String],
  reps: Option[Formula],
  time: Option[Measurement],
  score: Option[String],
  tasks: Option[Seq[core.SubTask]],
  createdAt: ZonedDateTime,
  createdByAccount: UUID,
  deletedAt: Option[ZonedDateTime],
  deletedByAccount: Option[UUID]
)

case class SubTask(
  reps: Option[Formula],
  movement: Option[TaskMovement]
)

case class TaskMovement(
  guid: UUID,
  name: String,
  score: Option[String],
  variables: Option[Seq[Variable]]
)
