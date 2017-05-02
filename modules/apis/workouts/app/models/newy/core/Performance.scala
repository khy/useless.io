package models.workouts.newy.core

import java.util.UUID

case class Performance(
  workoutGuid: UUID,
  variables: Option[Seq[BoundVariable]],
  tasks: Seq[ConcreteTask]
)
