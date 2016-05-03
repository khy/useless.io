package db.haiku

import java.util.UUID
import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

case class HaikuRecord(
  id: Long,
  guid: UUID,
  lineOne: String,
  lineTwo: String,
  lineThree: String,
  inResponseToId: Long,
  attribution: String,
  createdAt: Timestamp,
  createdByAccount: UUID,
  createdByAccessToken: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

class HaikusTable(tag: Tag)
  extends Table[HaikuRecord](tag, "haikus")
{
  def id = column[Long]("id")
  def guid = column[UUID]("guid")
  def lineOne = column[String]("line_one")
  def lineTwo = column[String]("line_two")
  def lineThree = column[String]("line_three")
  def inResponseToId = column[Long]("in_response_to_id")
  def attribution = column[String]("attribution")
  def createdAt = column[Timestamp]("created_at")
  def createdByAccount = column[UUID]("created_by_account")
  def createdByAccessToken = column[UUID]("created_by_access_token")
  def deletedAt = column[Option[Timestamp]]("deleted_at")
  def deletedByAccount = column[Option[UUID]]("deleted_by_account")
  def deletedByAccessToken = column[Option[UUID]]("deleted_by_access_token")

  def * = (id, guid, lineOne, lineTwo, lineThree, inResponseToId, attribution, createdAt, createdByAccount, createdByAccessToken, deletedAt, deletedByAccount, deletedByAccessToken) <> (HaikuRecord.tupled, HaikuRecord.unapply)
}

object Haikus extends TableQuery(new HaikusTable(_))
