package db.budget

import java.util.UUID
import java.sql.Timestamp

import slick.driver.PostgresDriver.api._

case class AccountRecord(
  id: Long,
  guid: UUID,
  typeKey: String,
  name: String,
  initialBalance: Option[BigDecimal],
  createdAt: Timestamp,
  createdByAccount: UUID,
  deletedAt: Option[Timestamp],
  deletedByAccount: Option[UUID]
)

class AccountsTable(tag: Tag)
  extends Table[AccountRecord](tag, "accounts")
{
  def id = column[Long]("id")
  def guid = column[UUID]("guid")
  def typeKey = column[String]("type_key")
  def name = column[String]("name")
  def initialBalance = column[Option[BigDecimal]]("initial_balance")
  def createdAt = column[Timestamp]("created_at")
  def createdByAccount = column[UUID]("created_by_account")
  def deletedAt = column[Option[Timestamp]]("deleted_at")
  def deletedByAccount = column[Option[UUID]]("deleted_by_account")

  def * = (id, guid, typeKey, name, initialBalance, createdAt, createdByAccount, deletedAt, deletedByAccount) <> (AccountRecord.tupled, AccountRecord.unapply)
}

object Accounts extends TableQuery(new AccountsTable(_))
