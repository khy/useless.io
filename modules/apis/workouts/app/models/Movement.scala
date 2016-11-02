package models.workouts

import java.util.UUID

case class Movement(
  guid: UUID,
  name: String,
  variables: Option[Seq[Variable]]
)
