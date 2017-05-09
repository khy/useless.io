package test.workouts

import java.util.UUID
import play.api.{ApplicationLoader, Environment}
import play.api.libs.ws.WS
import org.scalatestplus.play._
import io.useless.account.User
import io.useless.accesstoken.AccessToken
import io.useless.client.accesstoken.{AccessTokenClient, MockAccessTokenClient}
import io.useless.client.account.{AccountClient, MockAccountClient}

import test.workouts.old.TestHelper

trait IntegrationSpec
  extends PlaySpec
  with OneServerPerSuite
{

  lazy val applicationComponents = {
    new TestApplicationComponents(
      context = ApplicationLoader.createContext(Environment.simple()),
      accountClient = accountClient,
      accessTokenClient = accessTokenClient
    )
  }

  lazy val accessTokenClient: AccessTokenClient = new MockAccessTokenClient(accessTokens)

  lazy val accountClient: AccountClient = {
    val accounts = accessTokens.map(_.resourceOwner) ++
      accessTokens.map(_.client).filter(_.isDefined).map(_.get)

    new MockAccountClient(accounts)
  }

  val khyAccessToken = AccessToken(
    guid = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    resourceOwner = User(
      guid = UUID.fromString("00000000-1111-1111-1111-111111111111"),
      handle = "khy",
      name = None
    ),
    client = None,
    scopes = Seq()
  )

  val accessTokens = Seq(khyAccessToken)

  implicit val defaultAccessToken: AccessToken = khyAccessToken

  override implicit lazy val app = applicationComponents.application

  lazy val testHelper = new TestHelper(applicationComponents)

  def unauthenticatedRequest(path: String) = {
    val url = if (path.startsWith("http")) {
      path
    } else {
      s"http://localhost:${port}${path}"
    }

    applicationComponents.wsClient.url(url)
  }

  def request(path: String)(implicit accessToken: AccessToken) = {
    unauthenticatedRequest(path).withHeaders(("Authorization" -> accessToken.guid.toString))
  }

}
