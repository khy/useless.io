package db.budget

import java.util.UUID
import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

case class TransferRecord(
  id: Long,
  guid: UUID,
  fromTransactionId: Long,
  toTransactionId: Long,
  createdAt: Timestamp,
  createdByAccount: UUID,
  createdByAccessToken: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

class TransfersTable(tag: Tag)
  extends Table[TransferRecord](tag, "transfers")
{
  def id = column[Long]("id")
  def guid = column[UUID]("guid")
  def fromTransactionId = column[Long]("from_transaction_id")
  def toTransactionId = column[Long]("to_transaction_id")
  def createdAt = column[Timestamp]("created_at")
  def createdByAccount = column[UUID]("created_by_account")
  def createdByAccessToken = column[UUID]("created_by_access_token")
  def deletedAt = column[Option[Timestamp]]("deleted_at")
  def deletedByAccount = column[Option[UUID]]("deleted_by_account")
  def deletedByAccessToken = column[Option[UUID]]("deleted_by_access_token")

  def * = (id, guid, fromTransactionId, toTransactionId, createdAt, createdByAccount, createdByAccessToken, deletedAt, deletedByAccount, deletedByAccessToken) <> (TransferRecord.tupled, TransferRecord.unapply)
}

object Transfers extends TableQuery(new TransfersTable(_))
