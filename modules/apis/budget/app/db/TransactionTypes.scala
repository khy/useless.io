package db.budget

import java.util.UUID
import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

case class TransactionTypeRecord(
  id: Long,
  guid: UUID,
  name: String,
  ownershipKey: String,
  adjustedTransactionTypeId: Option[Long],
  createdAt: Timestamp,
  createdByAccount: UUID,
  createdByAccessToken: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

class TransactionTypesTable(tag: Tag)
  extends Table[TransactionTypeRecord](tag, "transaction_types")
{
  def id = column[Long]("id")
  def guid = column[UUID]("guid")
  def name = column[String]("name")
  def ownershipKey = column[String]("ownership_key")
  def adjustedTransactionTypeId = column[Option[Long]]("adjusted_transaction_type_id")
  def createdAt = column[Timestamp]("created_at")
  def createdByAccount = column[UUID]("created_by_account")
  def createdByAccessToken = column[UUID]("created_by_access_token")
  def deletedAt = column[Option[Timestamp]]("deleted_at")
  def deletedByAccount = column[Option[UUID]]("deleted_by_account")
  def deletedByAccessToken = column[Option[UUID]]("deleted_by_access_token")

  def * = (id, guid, name, ownershipKey, adjustedTransactionTypeId, createdAt, createdByAccount, createdByAccessToken, deletedAt, deletedByAccount, deletedByAccessToken) <> (TransactionTypeRecord.tupled, TransactionTypeRecord.unapply)
}

object TransactionTypes extends TableQuery(new TransactionTypesTable(_))
