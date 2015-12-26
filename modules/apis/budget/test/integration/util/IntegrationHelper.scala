package test.budget.integration.util

import java.util.UUID
import play.api.test.FakeApplication
import play.api.libs.ws.WS
import org.scalatestplus.play.OneServerPerSuite

import io.useless.client.accesstoken.{AccessTokenClient, MockAccessTokenClient}
import io.useless.client.account.{AccountClient, MockAccountClient}

import services.budget.TestService

trait IntegrationHelper {

  self: OneServerPerSuite =>

  implicit override lazy val app: FakeApplication = {
    FakeApplication(additionalConfiguration = Map("application.router" -> "budget.Routes"))
  }

  val mockAccessTokenClient = new MockAccessTokenClient(Seq(TestService.accessToken))
  val mockAccountClient = new MockAccountClient(Seq(TestService.accessToken.resourceOwner))

  AccessTokenClient.setMock(mockAccessTokenClient)
  AccountClient.setMock(mockAccountClient)

  def authenticatedRequest(path: String) = {
    unauthentictedRequest(path).withHeaders(("Authorization" -> TestService.accessToken.guid.toString))
  }

  def unauthentictedRequest(path: String) = {
    // Not sure why, but I need to explicitly pass the app here.
    WS.url(s"http://localhost:$port$path")(app)
  }

}
