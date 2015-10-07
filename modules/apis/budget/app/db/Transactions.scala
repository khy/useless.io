package db.budget

import java.util.UUID
import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

case class TransactionRecord(
  id: Long,
  guid: UUID,
  transactionTypeId: Long,
  accountId: Long,
  amount: BigDecimal,
  timestamp: Timestamp,
  adjustedTransactionId: Option[Long],
  createdAt: Timestamp,
  createdByAccount: UUID,
  createdByAccessToken: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

class TransactionsTable(tag: Tag)
  extends Table[TransactionRecord](tag, "transactions")
{
  def id = column[Long]("id")
  def guid = column[UUID]("guid")
  def transactionTypeId = column[Long]("transaction_type_id")
  def accountId = column[Long]("account_id")
  def amount = column[BigDecimal]("amount")
  def timestamp = column[Timestamp]("timestamp")
  def adjustedTransactionId = column[Option[Long]]("adjusted_transaction_id")
  def createdAt = column[Timestamp]("created_at")
  def createdByAccount = column[UUID]("created_by_account")
  def createdByAccessToken = column[UUID]("created_by_access_token")
  def deletedAt = column[Option[Timestamp]]("deleted_at")
  def deletedByAccount = column[Option[UUID]]("deleted_by_account")
  def deletedByAccessToken = column[Option[UUID]]("deleted_by_access_token")

  def * = (id, guid, transactionTypeId, accountId, amount, timestamp, adjustedTransactionId, createdAt, createdByAccount, createdByAccessToken, deletedAt, deletedByAccount, deletedByAccessToken) <> (TransactionRecord.tupled, TransactionRecord.unapply)
}

object Transactions extends TableQuery(new TransactionsTable(_))
