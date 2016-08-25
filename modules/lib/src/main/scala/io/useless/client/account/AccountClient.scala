package io.useless.client.account

import java.util.UUID
import scala.concurrent.Future
import play.api.{Application, BuiltInComponents}
import play.api.libs.ws.WSClient
import play.api.libs.ws.ning.NingWSComponents

import io.useless.client.Mockable
import io.useless.account.Account
import io.useless.util.configuration.RichConfiguration._

object AccountClient extends Mockable[AccountClient] {

  def instance(
    client: WSClient,
    baseUrl: String,
    authGuid: UUID
  )(implicit app: Application): AccountClient = {
    mock.getOrElse(new PlayAccountClient(client, baseUrl, authGuid))
  }

}

trait AccountClientComponent {

  self: NingWSComponents with BuiltInComponents =>

  val accountClientBaseUrl = configuration.underlying.getString("useless.core.baseUrl")
  val accountClientAuthGuid = configuration.underlying.getUuid("useless.core.authGuid")

  val accountClient: AccountClient = {
    new PlayAccountClient(wsClient, accountClientBaseUrl, accountClientAuthGuid)
  }

}

trait AccountClient {

  def getAccount(guid: UUID): Future[Option[Account]]

  def getAccountForEmail(email: String): Future[Option[Account]]

  def getAccountForHandle(handle: String): Future[Option[Account]]

}
