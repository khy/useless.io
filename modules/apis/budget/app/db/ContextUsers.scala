package db.budget

import java.util.UUID
import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

case class ContextUserRecord(
  id: Long,
  contextId: Long,
  userGuid: UUID,
  createdAt: Timestamp,
  createdByAccount: UUID,
  createdByAccessToken: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

class ContextUsersTable(tag: Tag)
  extends Table[ContextUserRecord](tag, "context_users")
{
  def id = column[Long]("id")
  def contextId = column[Long]("context_id")
  def userGuid = column[UUID]("user_guid")
  def createdAt = column[Timestamp]("created_at")
  def createdByAccount = column[UUID]("created_by_account")
  def createdByAccessToken = column[UUID]("created_by_access_token")
  def deletedAt = column[Option[Timestamp]]("deleted_at")
  def deletedByAccount = column[Option[UUID]]("deleted_by_account")
  def deletedByAccessToken = column[Option[UUID]]("deleted_by_access_token")

  def * = (id, contextId, userGuid, createdAt, createdByAccount, createdByAccessToken, deletedAt, deletedByAccount, deletedByAccessToken) <> (ContextUserRecord.tupled, ContextUserRecord.unapply)
}

object ContextUsers extends TableQuery(new ContextUsersTable(_))
