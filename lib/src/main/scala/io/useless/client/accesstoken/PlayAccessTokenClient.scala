package io.useless.client.accesstoken

import java.util.UUID
import play.api.libs.json.Json

import io.useless.accesstoken.AccessToken
import io.useless.play.client.ResourceClient
import io.useless.play.json.accesstoken.AccessTokenJson._
import io.useless.util.configuration.Configuration

class PlayAccessTokenClient(
  authGuid: UUID
) extends AccessTokenClient with Configuration {

  protected lazy val resourceClient = {
    val baseUrl = configuration.underlying.getString("useless.core.baseUrl")
    ResourceClient(baseUrl, authGuid.toString)
  }

  def getAccessToken(guid: UUID) = {
    val path = "/access_tokens/%s".format(guid.toString)
    resourceClient.get(path)
  }

}
