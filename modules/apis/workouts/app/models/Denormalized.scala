package models.workouts.denormalized

import models.workouts.{Measurement, Variable}

case class Scale()

case class Movement(
  name: String,
  score: Option[String],
  variables: Option[Seq[Variable]]
)

case class Task(
  name: Option[String],
  scales: Option[Seq[Scale]],
  reps: Option[Int],
  time: Option[Measurement],
  variables: Option[Seq[Variable]],
  score: Option[String],
  tasks: Option[Seq[Task]],
  movement: Option[Movement]
)
