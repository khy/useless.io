package models.workouts.newy

import java.util.UUID

case class Performance(
  guid: UUID,
  workoutGuid: UUID,
  variables: Option[Seq[BoundVariable]],
  tasks: Seq[ConcreteTask]
)
