package clients.auth.account

import java.util.UUID
import scala.concurrent.Future
import play.api.Play

import io.useless.play.client.ResourceClient
import io.useless.accesstoken.AccessToken
import io.useless.account.{ App, AuthorizedApp, User }
import io.useless.util.configuration.Configuration

object AccountClient
  extends DefaultAccountClientComponent
  with    Configuration
{

  def instance(optAccessToken: Option[AccessToken] = None): AccountClient = {
    val baseUrl = configuration.underlying.getString("useless.core.baseUrl")
    val auth = optAccessToken.map(_.guid.toString).getOrElse {
      configuration.underlying.getString("account.accessTokenGuid")
    }
    val resourceClient = ResourceClient(baseUrl, auth)
    new DefaultAccountClient(resourceClient)
  }

}

trait AccountClient {

  def getApp(guid: UUID): Future[Option[AuthorizedApp]]

  def getUserForEmail(email: String): Future[Option[User]]

  def createUser(email: String, handle: Option[String], name: Option[String]): Future[Either[String, User]]

}
