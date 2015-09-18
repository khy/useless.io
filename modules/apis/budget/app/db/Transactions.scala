package db.budget

import java.util.UUID
import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

case class TransactionRecord(
  id: Long,
  guid: UUID,
  transactionTypeId: Long,
  amount: BigDecimal,
  timestamp: Timestamp,
  createdAt: Timestamp,
  createdByAccount: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID]
)

class TransactionsTable(tag: Tag)
  extends Table[TransactionRecord](tag, "transactions")
{
  def id = column[Long]("id")
  def guid = column[UUID]("guid")
  def transactionTypeId = column[Long]("transaction_type_id")
  def amount = column[BigDecimal]("amount")
  def timestamp = column[Timestamp]("timestamp")
  def createdAt = column[Timestamp]("created_at")
  def createdByAccount = column[UUID]("created_by_account")
  def deletedAt = column[Option[Timestamp]]("deleted_at")
  def deletedByAccount = column[Option[UUID]]("deleted_by_account")

  def * = (id, guid, transactionTypeId, amount, timestamp, createdAt, createdByAccount, deletedAt, deletedByAccount) <> (TransactionRecord.tupled, TransactionRecord.unapply)
}

object Transactions extends TableQuery(new TransactionsTable(_))
