package io.useless.client.accesstoken

import play.api.BuiltInComponents
import play.api.libs.ws.ning.NingWSComponents

import io.useless.util.configuration.RichConfiguration._

trait AccessTokenClientComponents {

  def accessTokenClient: AccessTokenClient

}

trait DefaultAccessTokenClientComponents extends AccessTokenClientComponents {

  self: NingWSComponents with BuiltInComponents =>

  val accessTokeClientBaseUrl = configuration.underlying.getString("useless.core.baseUrl")
  val accessTokenClientAuthGuid = configuration.underlying.getUuid("useless.core.authGuid")

  val accessTokenClient: AccessTokenClient = {
    new PlayAccessTokenClient(wsClient, accessTokeClientBaseUrl, accessTokenClientAuthGuid)
  }

}
