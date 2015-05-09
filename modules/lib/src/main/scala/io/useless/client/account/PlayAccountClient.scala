package io.useless.client.account

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Application
import play.api.libs.json.Json

import io.useless.account.Account
import io.useless.play.client.ResourceClient
import io.useless.play.json.account.AccountJson._
import io.useless.util.configuration.Configuration

class PlayAccountClient(
  authGuid: UUID
)(
  implicit app: Application
) extends AccountClient with Configuration {

  protected lazy val resourceClient = {
    val baseUrl = configuration.underlying.getString("useless.core.baseUrl")
    ResourceClient(baseUrl, authGuid.toString)
  }

  def getAccount(guid: UUID) = {
    val path = "/accounts/%s".format(guid.toString)
    resourceClient.get(path)
  }

  def getAccountForEmail(email: String) = {
    resourceClient.find("/accounts", "email" -> email).map(_.items.headOption)
  }

  def getAccountForHandle(handle: String) = {
    resourceClient.find("/accounts", "handle" -> handle).map(_.items.headOption)
  }

}
