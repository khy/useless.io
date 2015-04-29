package io.useless.client.accesstoken

import java.util.UUID
import play.api.libs.json.Json

import io.useless.accesstoken.AccessToken
import io.useless.play.client.ResourceClient
import io.useless.play.json.accesstoken.AccessTokenJson._

class PlayAccessTokenClient(
  optAuthGuid: Option[UUID]
) extends AccessTokenClient with ResourceClient {

  private lazy val _resourceClient = optAuthGuid.map { authGuid =>
    resourceClient.withAuth(authGuid.toString)
  }.getOrElse(resourceClient)

  def getAccessToken(guid: UUID) = {
    val path = "/access_tokens/%s".format(guid.toString)
    _resourceClient.get(path)
  }

}
