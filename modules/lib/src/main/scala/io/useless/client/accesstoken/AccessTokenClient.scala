package io.useless.client.accesstoken

import java.util.UUID
import scala.concurrent.Future
import play.api.{Application, BuiltInComponents}
import play.api.libs.ws.WSClient
import play.api.libs.ws.ning.NingWSComponents

import io.useless.client.Mockable
import io.useless.accesstoken.AccessToken
import io.useless.util.configuration.RichConfiguration._

object AccessTokenClient extends Mockable[AccessTokenClient] {

  def instance(
    client: WSClient,
    baseUrl: String,
    authGuid: UUID
  ): AccessTokenClient = {
    mock.getOrElse(new PlayAccessTokenClient(client, baseUrl, authGuid))
  }

}

trait AccessTokenClientComponent {

  self: NingWSComponents with BuiltInComponents =>

  val accessTokeClientBaseUrl = configuration.underlying.getString("useless.core.baseUrl")
  val accessTokenClientAuthGuid = configuration.underlying.getUuid("useless.core.authGuid")

  val accessTokenClient: AccessTokenClient = {
    new PlayAccessTokenClient(wsClient, accessTokeClientBaseUrl, accessTokenClientAuthGuid)
  }

}

trait AccessTokenClient {

  def getAccessToken(guid: UUID): Future[Option[AccessToken]]

}
