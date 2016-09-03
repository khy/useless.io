package io.useless.client.account

import java.util.UUID
import play.api.BuiltInComponents
import play.api.libs.ws.ning.NingWSComponents

import io.useless.util.configuration.RichConfiguration._

trait AccountClientComponents {

  def accountClient: AccountClient

}

trait DefaultAccountClientComponents extends AccountClientComponents {

  self: NingWSComponents with BuiltInComponents =>

  def accountClientBaseUrl: String
  def accountClientAuthGuid: UUID

  lazy val accountClient: AccountClient = {
    new PlayAccountClient(wsClient, accountClientBaseUrl, accountClientAuthGuid)
  }

}
