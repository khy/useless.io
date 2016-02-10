package db.budget

import java.util.UUID
import java.sql.Timestamp

import slick.driver.PostgresDriver.api._

case class AccountRecord(
  id: Long,
  guid: UUID,
  contextId: Long,
  accountTypeKey: String,
  name: String,
  initialBalance: BigDecimal,
  createdAt: Timestamp,
  createdByAccount: UUID,
  createdByAccessToken: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID],
  deletedByAccessToken: Option[UUID]
)

class AccountsTable(tag: Tag)
  extends Table[AccountRecord](tag, "accounts")
{
  def id = column[Long]("id")
  def guid = column[UUID]("guid")
  def contextId = column[Long]("context_id")
  def accountTypeKey = column[String]("account_type_key")
  def name = column[String]("name")
  def initialBalance = column[BigDecimal]("initial_balance")
  def createdAt = column[Timestamp]("created_at")
  def createdByAccount = column[UUID]("created_by_account")
  def createdByAccessToken = column[UUID]("created_by_access_token")
  def deletedAt = column[Option[Timestamp]]("deleted_at")
  def deletedByAccount = column[Option[UUID]]("deleted_by_account")
  def deletedByAccessToken = column[Option[UUID]]("deleted_by_access_token")

  def * = (id, guid, contextId, accountTypeKey, name, initialBalance, createdAt, createdByAccount, createdByAccessToken, deletedAt, deletedByAccount, deletedByAccessToken) <> (AccountRecord.tupled, AccountRecord.unapply)
}

object Accounts extends TableQuery(new AccountsTable(_))
