package io.useless.client.account

import java.util.UUID
import scala.concurrent.ExecutionContext
import play.api.Application
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import io.useless.account.Account
import io.useless.play.client.ResourceClient
import io.useless.play.json.account.AccountJson._
import io.useless.util.configuration.Configuration

class PlayAccountClient(
  client: WSClient,
  baseUrl: String,
  authGuid: UUID
) extends AccountClient {

  protected lazy val resourceClient = {
    ResourceClient(client, baseUrl, authGuid.toString)
  }

  def getAccount(guid: UUID)(implicit ec: ExecutionContext) = {
    resourceClient.get(s"/accounts/${guid}")
  }

  def findAccounts(guids: Seq[UUID])(implicit ec: ExecutionContext) = {
    resourceClient.find("/accounts", guids.map { guid =>
      "guid" -> guid.toString
    }:_*).map(_.items)
  }

  def getAccountForEmail(email: String)(implicit ec: ExecutionContext) = {
    resourceClient.find("/accounts", "email" -> email).map(_.items.headOption)
  }

  def getAccountForHandle(handle: String)(implicit ec: ExecutionContext) = {
    resourceClient.find("/accounts", "handle" -> handle).map(_.items.headOption)
  }

}
