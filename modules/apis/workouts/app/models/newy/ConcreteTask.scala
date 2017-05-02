package models.workouts.newy

import java.util.UUID

case class ConcreteTask(
  reps: Int,
  movementGuid: UUID,
  time: Option[Int],
  variables: Option[Seq[BoundVariable]]
)
