package db.budget

import java.util.UUID
import java.sql.{Timestamp, Date}
import slick.driver.PostgresDriver.api._

case class PlannedTransactionRecord(
  id: Long,
  guid: UUID,
  transactionTypeId: Long,
  accountId: Long,
  minAmount: Option[BigDecimal],
  maxAmount: Option[BigDecimal],
  minDate: Option[Date],
  maxDate: Option[Date],
  adjustedPlannedTransactionId: Option[Long],
  createdAt: Timestamp,
  createdByAccount: UUID,
  createdByAccessToken: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

class PlannedTransactionsTable(tag: Tag)
  extends Table[PlannedTransactionRecord](tag, "planned_transactions")
{
  def id = column[Long]("id")
  def guid = column[UUID]("guid")
  def transactionTypeId = column[Long]("transaction_type_id")
  def accountId = column[Long]("account_id")
  def minAmount = column[Option[BigDecimal]]("min_amount")
  def maxAmount = column[Option[BigDecimal]]("max_amount")
  def minDate = column[Option[Date]]("min_date")
  def maxDate = column[Option[Date]]("max_date")
  def adjustedPlannedTransactionId = column[Option[Long]]("adjusted_planned_transaction_id")
  def createdAt = column[Timestamp]("created_at")
  def createdByAccount = column[UUID]("created_by_account")
  def createdByAccessToken = column[UUID]("created_by_access_token")
  def deletedAt = column[Option[Timestamp]]("deleted_at")
  def deletedByAccount = column[Option[UUID]]("deleted_by_account")
  def deletedByAccessToken = column[Option[UUID]]("deleted_by_access_token")

  def * = (id, guid, transactionTypeId, accountId, minAmount, maxAmount, minDate, maxDate, adjustedPlannedTransactionId, createdAt, createdByAccount, createdByAccessToken, deletedAt, deletedByAccount, deletedByAccessToken) <> (PlannedTransactionRecord.tupled, PlannedTransactionRecord.unapply)
}

object PlannedTransactions extends TableQuery(new PlannedTransactionsTable(_))
