package io.useless.client.accesstoken

import java.util.UUID
import scala.concurrent.Future

import io.useless.client.Mockable
import io.useless.accesstoken.AccessToken

object AccessTokenClient extends Mockable[AccessTokenClient] {

  def instance(optAuthGuid: Option[UUID] = None): AccessTokenClient = {
    mock.getOrElse(new PlayAccessTokenClient(optAuthGuid))
  }

}

trait AccessTokenClient {

  def getAccessToken(guid: UUID): Future[Option[AccessToken]]

}
