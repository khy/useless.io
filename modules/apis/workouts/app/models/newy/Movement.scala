package models.workouts.newy

import java.util.UUID
import java.time.ZonedDateTime
import play.api.libs.json.Json

import models.workouts.newy.core.FreeVariable

case class Movement(
  guid: UUID,
  name: String,
  variables: Option[Seq[FreeVariable]],
  createdAt: ZonedDateTime,
  createdByAccount: UUID,
  deletedAt: Option[ZonedDateTime],
  deletedByAccount: Option[UUID]
)

object Movement {

  implicit val jsonFormat = Json.format[Movement]

}
