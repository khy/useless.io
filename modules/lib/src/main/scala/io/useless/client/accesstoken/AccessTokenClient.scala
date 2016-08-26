package io.useless.client.accesstoken

import java.util.UUID
import scala.concurrent.Future
import play.api.Application
import play.api.libs.ws.WSClient

import io.useless.client.Mockable
import io.useless.accesstoken.AccessToken

object AccessTokenClient extends Mockable[AccessTokenClient] {

  def instance(
    client: WSClient,
    baseUrl: String,
    authGuid: UUID
  ): AccessTokenClient = {
    mock.getOrElse(new PlayAccessTokenClient(client, baseUrl, authGuid))
  }

}

trait AccessTokenClient {

  def getAccessToken(guid: UUID): Future[Option[AccessToken]]

}
