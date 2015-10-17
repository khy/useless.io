package services.budget

import java.util.UUID
import scala.concurrent.ExecutionContext
import play.api.Play.current
import play.api.test.Helpers._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.PostgresDriver.api._
import org.joda.time.DateTime
import io.useless.account.User
import io.useless.accesstoken.AccessToken

import models.budget._
import db.budget._
import db.budget.util.DatabaseAccessor

object TestService extends DatabaseAccessor {

  lazy val accountsService = AccountsService.default()
  lazy val transactionTypesService = TransactionTypesService.default()
  lazy val plannedTransactionsService = PlannedTransactionsService.default()
  lazy val transactionsService = TransactionsService.default()

  val accessToken = AccessToken(
    guid = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    resourceOwner = User(
      guid = UUID.fromString("00000000-1111-1111-1111-111111111111"),
      handle = "khy",
      name = None
    ),
    client = None,
    scopes = Seq()
  )

  val otherAccessToken = AccessToken(
    guid = UUID.fromString("22222222-2222-2222-2222-222222222222"),
    resourceOwner = User(
      guid = UUID.fromString("33333333-3333-3333-3333-333333333333"),
      handle = "mike",
      name = None
    ),
    client = None,
    scopes = Seq()
  )

  def createAccount(
    accountType: AccountType = AccountType.Checking,
    name: String = "Test Account",
    initialBalance: BigDecimal = 100.10,
    accessToken: AccessToken = accessToken
  ): Account = await {
    accountsService.createAccount(accountType, name, initialBalance, accessToken)
  }.toSuccess.value

  def deleteAccounts() {
    deleteTransactionTypes()
    val query = Accounts.filter { a => a.id === a.id }
    await { database.run(query.delete) }
  }

  def createTransactionType(
    name: String = "Rent",
    parentGuid: Option[UUID] = None,
    ownership: TransactionTypeOwnership = TransactionTypeOwnership.User,
    accessToken: AccessToken = accessToken
  ): TransactionType = await {
    transactionTypesService.createTransactionType(name, parentGuid, ownership, accessToken)
  }.toSuccess.value

  def getSystemTransactionType(name: String): TransactionType = await {
    transactionTypesService.findTransactionTypes(
      names = Some(Seq(name)), ownerships = Some(Seq(TransactionTypeOwnership.System))
    )
  }.toSuccess.value.items.head

  def deleteTransactionTypes() {
    deleteTransactions()
    deletePlannedTransactions()
    val query = TransactionTypes.filterNot { transactionType =>
      transactionType.ownershipKey === TransactionTypeOwnership.System.key &&
      transactionType.createdByAccount === UUID.fromString("2a436fb0-7336-4f19-bde7-61570c05640c")
    }
    await { database.run(query.delete) }
  }

  def createPlannedTransaction(
    transactionTypeGuid: UUID = createTransactionType().guid,
    accountGuid: UUID = createAccount().guid,
    minAmount: Option[BigDecimal] = Some(100.00),
    maxAmount: Option[BigDecimal] = Some(200.00),
    minTimestamp: Option[DateTime] = Some(DateTime.now.plusDays(10)),
    maxTimestamp: Option[DateTime] = Some(DateTime.now.plusDays(15)),
    accessToken: AccessToken = accessToken
  ): PlannedTransaction = await {
    plannedTransactionsService.createPlannedTransaction(transactionTypeGuid, accountGuid, minAmount, maxAmount, minTimestamp, maxTimestamp, accessToken)
  }.toSuccess.value

  def deletePlannedTransactions() {
    deleteTransactions()
    val query = PlannedTransactions.filter { a => a.id === a.id }
    await { database.run(query.delete) }
  }

  def createTransaction(
    transactionTypeGuid: UUID = createTransactionType().guid,
    accountGuid: UUID = createAccount().guid,
    amount: BigDecimal = 100.00,
    timestamp: DateTime = DateTime.now.minusDays(1),
    plannedTransactionGuid: Option[UUID] = None,
    adjustedTransactionGuid: Option[UUID] = None,
    accessToken: AccessToken = accessToken
  ): Transaction = await {
    val futResult = transactionsService.createTransaction(transactionTypeGuid, accountGuid, amount, timestamp, plannedTransactionGuid, adjustedTransactionGuid, accessToken)
    futResult.flatMap { result =>
      transactionsService.records2models(Seq(result.toSuccess.value))
    }
  }.head

  def softDeleteTransaction(
    transactionGuid: UUID,
    accessToken: AccessToken = accessToken
  ): Boolean = await {
    transactionsService.deleteTransaction(transactionGuid, accessToken)
  }.toSuccess.value

  def deleteTransactions() {
    deleteTransfers()
    val query = Transactions.filter { a => a.id === a.id }
    await { database.run(query.delete) }
  }

  def deleteTransfers() {
    val query = Transfers.filter { r => r.id === r.id }
    await { database.run(query.delete) }
  }

}
