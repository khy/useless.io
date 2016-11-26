package db.workouts

import java.util.UUID
import java.time.ZonedDateTime
import play.api.libs.json.JsValue

import Driver.api._

case class WorkoutRecord(
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

class WorkoutTable(tag: Tag)
  extends Table[WorkoutRecord](tag, "workouts")
  with SchemaData[WorkoutRecord]
  with AuditData[WorkoutRecord]
{
  def guid = column[UUID]("guid")
  def json = column[JsValue]("json")

  def * = (guid, schemaVersionMajor, schemaVersionMinor, json, createdAt,
    createdByAccount, createdByAccessToken, deletedAt, deletedByAccount,
    deletedByAccessToken) <> (WorkoutRecord.tupled, WorkoutRecord.unapply)
}

object Workouts extends TableQuery(new WorkoutTable(_))
