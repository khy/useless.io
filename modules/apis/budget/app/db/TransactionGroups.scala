package db.budget

import java.util.UUID
import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

case class TransactionGroupRecord(
  id: Long,
  guid: UUID,
  accountId: Long,
  transactionTypeKey: String,
  name: String,
  createdAt: Timestamp,
  createdByAccount: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID]
)

class TransactionGroupsTable(tag: Tag)
  extends Table[TransactionGroupRecord](tag, "transaction_groups")
{
  def id = column[Long]("id")
  def guid = column[UUID]("guid")
  def accountId = column[Long]("account_id")
  def transactionTypeKey = column[String]("transaction_type_key")
  def name = column[String]("name")
  def createdAt = column[Timestamp]("created_at")
  def createdByAccount = column[UUID]("created_by_account")
  def deletedAt = column[Option[Timestamp]]("deleted_at")
  def deletedByAccount = column[Option[UUID]]("deleted_by_account")

  def * = (id, guid, accountId, transactionTypeKey, name, createdAt, createdByAccount, deletedAt, deletedByAccount) <> (TransactionGroupRecord.tupled, TransactionGroupRecord.unapply)
}

object TransactionGroups extends TableQuery(new TransactionGroupsTable(_))
