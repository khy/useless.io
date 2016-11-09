package db.workouts

import java.util.UUID
import java.time.ZonedDateTime
import play.api.libs.json.JsValue

import Driver.api._

case class MovementRecord(
  guid: UUID,
  name: String,
  variables: Option[JsValue],
  createdAt: ZonedDateTime,
  createdByAccount: UUID,
  createdByAccessToken: UUID,
  deletedAt: Option[ZonedDateTime],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

class MovementTable(tag: Tag)
  extends Table[MovementRecord](tag, "movements")
  with AuditData[MovementRecord]
{
  def guid = column[UUID]("guid")
  def variables = column[Option[JsValue]]("variables")
  def name = column[String]("name")

  def * = (guid, name, variables, createdAt, createdByAccount,
    createdByAccessToken, deletedAt, deletedByAccount,
    deletedByAccessToken) <> (MovementRecord.tupled, MovementRecord.unapply)
}

object Movements extends TableQuery(new MovementTable(_))
