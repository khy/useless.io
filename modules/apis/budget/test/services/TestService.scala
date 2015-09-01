package services.budget

import java.util.UUID
import scala.concurrent.ExecutionContext
import play.api.Play.current
import play.api.test.Helpers._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import io.useless.account.User
import io.useless.accesstoken.AccessToken

import models.budget.{Account, AccountType}

object TestService {

  lazy val accountsService = AccountsService.default()

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

  def createAccount(
    accountType: AccountType = AccountType.Checking,
    name: String = "Test Account",
    initialBalance: Option[BigDecimal] = None,
    accessToken: AccessToken = accessToken
  ): Account = await {
    accountsService.createAccount(accountType, name, initialBalance, accessToken)
  }.toSuccess.value

}
