package db.budget

import java.util.UUID
import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

case class TransactionConfirmationRecord(
  id: Long,
  guid: UUID,
  transactionId: Long,
  createdAt: Timestamp,
  createdByAccount: UUID,
  createdByAccessToken: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

class TransactionConfirmationsTable(tag: Tag)
  extends Table[TransactionConfirmationRecord](tag, "transaction_confirmations")
{
  def id = column[Long]("id")
  def guid = column[UUID]("guid")
  def transactionId = column[Long]("transaction_id")
  def createdAt = column[Timestamp]("created_at")
  def createdByAccount = column[UUID]("created_by_account")
  def createdByAccessToken = column[UUID]("created_by_access_token")
  def deletedAt = column[Option[Timestamp]]("deleted_at")
  def deletedByAccount = column[Option[UUID]]("deleted_by_account")
  def deletedByAccessToken = column[Option[UUID]]("deleted_by_access_token")

  def * = (id, guid, transactionId, createdAt, createdByAccount, createdByAccessToken, deletedAt, deletedByAccount, deletedByAccessToken) <> (TransactionConfirmationRecord.tupled, TransactionConfirmationRecord.unapply)
}

object TransactionConfirmations extends TableQuery(new TransactionConfirmationsTable(_))
