package db.budget

import java.util.UUID
import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

case class TransactionTransactionTypeRecord(
  id: Long,
  transactionId: Long,
  transactionTypeId: Long,
  adjustedTransactionTransactionTypeId: Option[Long],
  createdAt: Timestamp,
  createdByAccount: UUID,
  createdByAccessToken: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

class TransactionTransactionTypesTable(tag: Tag)
  extends Table[TransactionTransactionTypeRecord](tag, "transaction_transaction_types")
{
  def id = column[Long]("id")
  def transactionId = column[Long]("transaction_id")
  def transactionTypeId = column[Long]("transaction_type_id")
  def adjustedTransactionTransactionTypeId = column[Option[Long]]("adjusted_transaction_transaction_type_id")
  def createdAt = column[Timestamp]("created_at")
  def createdByAccount = column[UUID]("created_by_account")
  def createdByAccessToken = column[UUID]("created_by_access_token")
  def deletedAt = column[Option[Timestamp]]("deleted_at")
  def deletedByAccount = column[Option[UUID]]("deleted_by_account")
  def deletedByAccessToken = column[Option[UUID]]("deleted_by_access_token")

  def * = (id, transactionId, transactionTypeId, adjustedTransactionTransactionTypeId, createdAt, createdByAccount, createdByAccessToken, deletedAt, deletedByAccount, deletedByAccessToken) <> (TransactionTransactionTypeRecord.tupled, TransactionTransactionTypeRecord.unapply)
}

object TransactionTransactionTypes extends TableQuery(new TransactionTransactionTypesTable(_))
