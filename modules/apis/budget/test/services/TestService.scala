package services.budget

import java.util.UUID
import scala.concurrent.ExecutionContext
import play.api.Play.current
import play.api.test.Helpers._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.PostgresDriver.api._
import io.useless.account.User
import io.useless.accesstoken.AccessToken

import models.budget._
import db.budget._
import db.budget.util.DatabaseAccessor

object TestService extends DatabaseAccessor {

  lazy val accountsService = AccountsService.default()
  lazy val projectionsService = ProjectionsService.default()
  lazy val transactionTypesService = TransactionTypesService.default()

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

  def createProjection(
    name: String = "My Projection",
    accessToken: AccessToken = accessToken
  ): Projection = await {
    projectionsService.createProjection(name, accessToken)
  }.toSuccess.value

  def deleteProjections() {
    val query = Projections.filter { a => a.id === a.id }
    await { database.run(query.delete) }
  }

  def createTransactionType(
    transactionType: TransactionClass = TransactionClass.Income,
    accountGuid: UUID = createAccount().guid,
    name: String =  "Rent"
  ): TransactionType = await {
    transactionTypesService.createTransactionType(accountGuid, transactionType, name, accessToken)
  }.toSuccess.value

  def deleteTransactionTypes() {
    deleteTransactions()
    val query = TransactionTypes.filter { a => a.id === a.id }
    await { database.run(query.delete) }
  }

  def deleteTransactions() {
    val query = Transactions.filter { a => a.id === a.id }
    await { database.run(query.delete) }
  }

}
