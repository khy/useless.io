package io.useless.client.accesstoken

import java.util.UUID
import play.api.BuiltInComponents
import play.api.libs.ws.ning.NingWSComponents

import io.useless.util.configuration.RichConfiguration._

trait AccessTokenClientComponents {

  def accessTokenClient: AccessTokenClient

}

trait DefaultAccessTokenClientComponents extends AccessTokenClientComponents {

  self: NingWSComponents with BuiltInComponents =>

  def accessTokenClientBaseUrl: String
  def accessTokenClientAuthGuid: UUID

  lazy val accessTokenClient: AccessTokenClient = {
    new PlayAccessTokenClient(wsClient, accessTokenClientBaseUrl, accessTokenClientAuthGuid)
  }

}
