package io.useless.client.account

import play.api.BuiltInComponents
import play.api.libs.ws.ning.NingWSComponents

import io.useless.util.configuration.RichConfiguration._

trait AccountClientComponents {

  def accountClient: AccountClient

}

trait DefaultAccountClientComponents extends AccountClientComponents {

  self: NingWSComponents with BuiltInComponents =>

  val accountClientBaseUrl = configuration.underlying.getString("useless.core.baseUrl")
  val accountClientAuthGuid = configuration.underlying.getUuid("useless.core.authGuid")

  val accountClient: AccountClient = {
    new PlayAccountClient(wsClient, accountClientBaseUrl, accountClientAuthGuid)
  }

}
