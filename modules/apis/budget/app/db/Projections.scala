package db.budget

import java.util.UUID
import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

case class ProjectionRecord(
  id: Long,
  guid: UUID,
  name: String,
  createdAt: Timestamp,
  createdByAccount: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID]
)

class ProjectionsTable(tag: Tag)
  extends Table[ProjectionRecord](tag, "projections")
{
  def id = column[Long]("id")
  def guid = column[UUID]("guid")
  def name = column[String]("name")
  def createdAt = column[Timestamp]("created_at")
  def createdByAccount = column[UUID]("created_by_account")
  def deletedAt = column[Option[Timestamp]]("deleted_at")
  def deletedByAccount = column[Option[UUID]]("deleted_by_account")

  def * = (id, guid, name, createdAt, createdByAccount, deletedAt, deletedByAccount) <> (ProjectionRecord.tupled, ProjectionRecord.unapply)
}

object Projections extends TableQuery(new ProjectionsTable(_))
