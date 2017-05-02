package models.workouts.newy

import java.util.UUID

case class Variant(
  guid: UUID,
  workoutGuid: UUID,
  name: String,
  task: AbstractTask
)
