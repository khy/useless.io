package io.useless.client.account

import java.util.UUID
import scala.concurrent.Future
import play.api.Application

import io.useless.client.Mockable
import io.useless.account.Account

object AccountClient extends Mockable[AccountClient] {

  def instance(authGuid: UUID)(implicit app: Application): AccountClient = {
    mock.getOrElse(new PlayAccountClient(authGuid))
  }

}

trait AccountClient {

  def getAccount(guid: UUID): Future[Option[Account]]

  def getAccountForEmail(email: String): Future[Option[Account]]

  def getAccountForHandle(handle: String): Future[Option[Account]]

}
