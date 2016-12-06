package db.workouts

import java.util.UUID
import java.time.ZonedDateTime
import play.api.libs.json.JsValue

import Driver.api._

case class MovementRecord(
  guid: UUID,
  schemaVersionMajor: Int,
  schemaVersionMinor: Int,
  json: JsValue,
  createdAt: ZonedDateTime,
  createdByAccount: UUID,
  createdByAccessToken: UUID,
  deletedAt: Option[ZonedDateTime],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

class MovementsTable(tag: Tag)
  extends Table[MovementRecord](tag, "movements")
  with SchemaData[MovementRecord]
  with AuditData[MovementRecord]
{
  def guid = column[UUID]("guid")
  def json = column[JsValue]("json")

  def * = (guid, schemaVersionMajor, schemaVersionMinor, json, createdAt,
    createdByAccount, createdByAccessToken, deletedAt, deletedByAccount,
    deletedByAccessToken) <> (MovementRecord.tupled, MovementRecord.unapply)
}

object Movements extends TableQuery(new MovementsTable(_))
