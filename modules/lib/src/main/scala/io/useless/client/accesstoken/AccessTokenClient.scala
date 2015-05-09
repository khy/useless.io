package io.useless.client.accesstoken

import java.util.UUID
import scala.concurrent.Future
import play.api.Application

import io.useless.client.Mockable
import io.useless.accesstoken.AccessToken

object AccessTokenClient extends Mockable[AccessTokenClient] {

  def instance(authGuid: UUID)(implicit app: Application): AccessTokenClient = {
    mock.getOrElse(new PlayAccessTokenClient(authGuid))
  }

}

trait AccessTokenClient {

  def getAccessToken(guid: UUID): Future[Option[AccessToken]]

}
