package db.budget

import java.util.UUID
import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

case class TransactionTypeSubtypeRecord(
  id: Long,
  parentTransactionTypeId: Long,
  childTransactionTypeId: Long,
  adjustedTransactionTypeSubtypeId: Option[Long],
  createdAt: Timestamp,
  createdByAccount: UUID,
  createdByAccessToken: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

class TransactionTypeSubtypesTable(tag: Tag)
  extends Table[TransactionTypeSubtypeRecord](tag, "transaction_type_subtypes")
{
  def id = column[Long]("id")
  def parentTransactionTypeId = column[Long]("parent_transaction_type_id")
  def childTransactionTypeId = column[Long]("child_transaction_type_id")
  def adjustedTransactionTypeSubtypeId = column[Option[Long]]("adjusted_transaction_type_subtype_id")
  def createdAt = column[Timestamp]("created_at")
  def createdByAccount = column[UUID]("created_by_account")
  def createdByAccessToken = column[UUID]("created_by_access_token")
  def deletedAt = column[Option[Timestamp]]("deleted_at")
  def deletedByAccount = column[Option[UUID]]("deleted_by_account")
  def deletedByAccessToken = column[Option[UUID]]("deleted_by_access_token")

  def * = (id, parentTransactionTypeId, childTransactionTypeId, adjustedTransactionTypeSubtypeId, createdAt, createdByAccount, createdByAccessToken, deletedAt, deletedByAccount, deletedByAccessToken) <> (TransactionTypeSubtypeRecord.tupled, TransactionTypeSubtypeRecord.unapply)
}

object TransactionTypeSubtypes extends TableQuery(new TransactionTypeSubtypesTable(_))
