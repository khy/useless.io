package models.workouts.newy.core

import java.util.UUID
import play.api.libs.json.Json

case class Performance(
  workoutGuid: UUID,
  variables: Option[Seq[BoundVariable]],
  tasks: Seq[ConcreteTask]
)

object Performance {

  implicit val jsonFormat = Json.format[Performance]

}
