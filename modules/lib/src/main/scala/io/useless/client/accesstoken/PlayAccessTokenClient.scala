package io.useless.client.accesstoken

import java.util.UUID
import play.api.Application
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import io.useless.accesstoken.AccessToken
import io.useless.play.client.ResourceClient
import io.useless.play.json.accesstoken.AccessTokenJson._
import io.useless.util.configuration.Configuration

class PlayAccessTokenClient(
  client: WSClient,
  baseUrl: String,
  authGuid: UUID
) extends AccessTokenClient with Configuration {

  protected lazy val resourceClient = ResourceClient(client, baseUrl, authGuid.toString)

  def getAccessToken(guid: UUID) = {
    val path = "/access_tokens/%s".format(guid.toString)
    resourceClient.get(path)
  }

}
