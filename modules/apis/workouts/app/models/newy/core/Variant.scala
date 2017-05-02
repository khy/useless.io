package models.workouts.newy.core

import java.util.UUID

case class Variant(
  workoutGuid: UUID,
  name: String,
  task: AbstractTask
)
