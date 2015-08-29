package test.budget.integration.util

import java.util.UUID
import play.api.libs.ws.WS
import org.scalatestplus.play.OneServerPerSuite

import io.useless.accesstoken.AccessToken
import io.useless.account.User
import io.useless.client.accesstoken.{AccessTokenClient, MockAccessTokenClient}
import io.useless.client.account.{AccountClient, MockAccountClient}

object IntegrationHelper {

  val accessToken = AccessToken(
    guid = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    resourceOwner = User(
      guid = UUID.fromString("00000000-1111-1111-1111-111111111111"),
      handle = "khy",
      name = None
    ),
    client = None,
    scopes = Seq()
  )

}

trait IntegrationHelper {

  self: OneServerPerSuite =>

  val mockAccessTokenClient = new MockAccessTokenClient(Seq(IntegrationHelper.accessToken))
  val mockAccountClient = new MockAccountClient(Seq(IntegrationHelper.accessToken.resourceOwner))

  AccessTokenClient.setMock(mockAccessTokenClient)
  AccountClient.setMock(mockAccountClient)

  def authenticatedRequest(path: String) = {
    unauthentictedRequest(path).withHeaders(("Authorization" -> IntegrationHelper.accessToken.guid.toString))
  }

  def unauthentictedRequest(path: String) = {
    WS.url(s"http://localhost:$port$path")
  }

}
