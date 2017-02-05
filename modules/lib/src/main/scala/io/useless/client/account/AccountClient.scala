package io.useless.client.account

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import play.api.Application
import play.api.libs.ws.WSClient

import io.useless.client.Mockable
import io.useless.account.Account

object AccountClient extends Mockable[AccountClient] {

  def instance(
    client: WSClient,
    baseUrl: String,
    authGuid: UUID
  )(implicit app: Application): AccountClient = {
    mock.getOrElse(new PlayAccountClient(client, baseUrl, authGuid))
  }

}

trait AccountClient {

  def getAccount(guid: UUID)(implicit ec: ExecutionContext): Future[Option[Account]]

  def findAccounts(guids: Seq[UUID])(implicit ec: ExecutionContext): Future[Seq[Account]]

  def getAccountForEmail(email: String)(implicit ec: ExecutionContext): Future[Option[Account]]

  def getAccountForHandle(handle: String)(implicit ec: ExecutionContext): Future[Option[Account]]

}
