package db.core.social

import java.util.UUID
import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

case class LikeRecord(
  id: Long,
  guid: UUID,
  resourceApi: String,
  resourceType: String,
  resourceId: String,
  createdAt: Timestamp,
  createdByAccount: UUID,
  createdByAccessToken: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

class LikesTable(tag: Tag)
  extends Table[LikeRecord](tag, Some("social"), "likes")
{
  def id = column[Long]("id")
  def guid = column[UUID]("guid")
  def resourceApi = column[String]("resource_api")
  def resourceType = column[String]("resource_type")
  def resourceId = column[String]("resource_id")
  def createdAt = column[Timestamp]("created_at")
  def createdByAccount = column[UUID]("created_by_account")
  def createdByAccessToken = column[UUID]("created_by_access_token")
  def deletedAt = column[Option[Timestamp]]("deleted_at")
  def deletedByAccount = column[Option[UUID]]("deleted_by_account")
  def deletedByAccessToken = column[Option[UUID]]("deleted_by_access_token")

  def * = (id, guid, resourceApi, resourceType, resourceId, createdAt, createdByAccount, createdByAccessToken, deletedAt, deletedByAccount, deletedByAccessToken) <> (LikeRecord.tupled, LikeRecord.unapply)
}

object Likes extends TableQuery(new LikesTable(_))
