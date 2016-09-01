package test.util

import play.api.{ApplicationLoader, Environment}
import org.scalatestplus.play._
import io.useless.accesstoken.AccessToken

trait DefaultSpec
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

  val editionClient = new MockEditionClient(Seq.empty)

  override implicit lazy val app = applicationComponents.application

  lazy val appHelper = new AppHelper(applicationComponents)

  implicit val defaultAccessToken: AccessToken = khyAccessToken

}
