package clients.auth.account

import java.util.UUID
import scala.concurrent.Future

import io.useless.play.client.ResourceClient
import io.useless.accesstoken.AccessToken
import io.useless.account.{ App, AuthorizedApp, User }

object AccountClient
  extends DefaultAccountClientComponent
  with    ResourceClient
{

  lazy val instance: AccountClient = new DefaultAccountClient(resourceClient)

  def withAuth(accessToken: AccessToken) = {
    val newResourceClient = resourceClient.withAuth(accessToken.guid.toString)
    new DefaultAccountClient(newResourceClient)
  }

}

trait AccountClient {

  def getApp(guid: UUID): Future[Option[AuthorizedApp]]

  def getUserForEmail(email: String): Future[Option[User]]

  def createUser(email: String, handle: Option[String], name: Option[String]): Future[Either[String, User]]

}
