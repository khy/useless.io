package test.util

import play.api.{ApplicationLoader, Environment}
import play.api.libs.ws.WS
import org.scalatestplus.play._
import io.useless.accesstoken.AccessToken

trait IntegrationSpec
  extends PlaySpec
  with OneServerPerSuite
  with DefaultUselessCoreMock
{

  lazy val applicationComponents = {
    new TestApplicationComponents(
      context = ApplicationLoader.createContext(Environment.simple()),
      accountClient = accountClient,
      accessTokenClient = accessTokenClient,
      editionClient = editionClient
    )
  }

  val editionClient = MockEditionClient.default

  override implicit lazy val app = applicationComponents.application

  lazy val appHelper = new AppHelper(applicationComponents)

  implicit val defaultAccessToken: AccessToken = khyAccessToken

  def request(path: String)(implicit accessToken: AccessToken) = {
    val url = if (path.startsWith("http")) {
      path
    } else {
      s"http://localhost:${port}${path}"
    }

    WS.url(url).withHeaders(("Authorization" -> accessToken.guid.toString))
  }

}
