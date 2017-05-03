package models.workouts.core

import java.util.UUID
import play.api.libs.json.Json

case class Movement(
  name: String,
  variables: Option[Seq[FreeVariable]]
)

object Movement {

  implicit val jsonFormat = Json.format[Movement]

}
