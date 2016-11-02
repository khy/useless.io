package models.workouts

import java.util.UUID

sealed trait Task {
  def reps: Option[Int]
  def time: Option[Measurement]
  def variables: Option[Seq[Variable]]
  def score: Option[String]
  def tasks: Option[Seq[SubTask]]
  def movement: Option[TaskMovement]
}

case class Workout(
  guid: UUID,
  parentGuid: Option[UUID],
  name: Option[String],
  reps: Option[Int],
  time: Option[Measurement],
  variables: Option[Seq[Variable]],
  score: Option[String],
  tasks: Option[Seq[SubTask]],
  movement: Option[TaskMovement]
) extends Task

case class SubTask(
  reps: Option[Int],
  time: Option[Measurement],
  variables: Option[Seq[Variable]],
  score: Option[String],
  tasks: Option[Seq[SubTask]],
  movement: Option[TaskMovement]
) extends Task

case class TaskMovement(
  name: String,
  score: Option[String],
  variables: Option[Seq[Variable]]
)
