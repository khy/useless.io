package test.util

import play.api.{ApplicationLoader, Environment}
import org.scalatestplus.play._

trait DefaultSpec
  extends PlaySpec
  with OneServerPerSuite
  with DefaultUselessMock
{

  lazy val applicationComponents = {
    new TestApplicationComponents(
      context = ApplicationLoader.createContext(Environment.simple()),
      accountClient = mockAccountClient,
      accessTokenClient = mockAccessTokenClient
    )
  }

  override implicit lazy val app = applicationComponents.application

  lazy val appHelper = new AppHelper(applicationComponents)

}
