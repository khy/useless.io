package models.workouts.old

import java.util.UUID
import java.time.ZonedDateTime
import io.useless.account.User

trait Task {
  def reps: Option[Formula]
  def time: Option[Measurement]
  def tasks: Option[Seq[Task]]
  def movement: Option[core.TaskMovement]
}

package core {

  case class Movement(
    name: String,
    variables: Option[Seq[Variable]]
  )

  case class Workout(
    name: Option[String],
    reps: Option[Formula],
    time: Option[Measurement],
    score: Option[String],
    tasks: Option[Seq[SubTask]],
    movement: Option[TaskMovement]
  ) extends Task

  case class SubTask(
    reps: Option[Formula],
    time: Option[Measurement],
    tasks: Option[Seq[SubTask]],
    movement: Option[TaskMovement]
  ) extends Task

  case class TaskMovement(
    guid: UUID,
    score: Option[String],
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
  parentGuids: Option[Seq[UUID]],
  name: Option[String],
  reps: Option[Formula],
  time: Option[Measurement],
  score: Option[String],
  tasks: Option[Seq[core.SubTask]],
  movement: Option[core.TaskMovement],
  createdAt: ZonedDateTime,
  createdBy: User,
  deletedAt: Option[ZonedDateTime],
  deletedBy: Option[User]
)
