package models.workouts.core

import java.util.UUID
import play.api.libs.json.Json

case class Variant(
  workoutGuid: UUID,
  name: String,
  task: AbstractTask
)

object Variant {

  implicit val jsonFormat = Json.format[Variant]

}
