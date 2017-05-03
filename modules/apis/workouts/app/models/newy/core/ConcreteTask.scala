package models.workouts.newy.core

import java.util.UUID
import play.api.libs.json.Json

case class ConcreteTask(
  reps: Int,
  movementGuid: UUID,
  time: Option[Int],
  variables: Option[Seq[BoundVariable]]
)

object ConcreteTask {

  implicit val jsonFormat = Json.format[ConcreteTask]

}
