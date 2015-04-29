package clients.auth.accesstoken

import java.util.UUID
import scala.concurrent.Future
import play.api.Play

import io.useless.play.client.ResourceClient
import io.useless.accesstoken.{ AccessToken, AuthorizedAccessToken, Scope }

object AccessTokenClient
  extends DefaultAccessTokenClientComponent
  with    ResourceClient
{

  lazy val instance: AccessTokenClient = {
    val config = Play.current.configuration
    val accessTokenGuid = config.getString("auth.accessTokenGuid").get
    val _resourceClient = resourceClient(accessTokenGuid)
    new DefaultAccessTokenClient(_resourceClient)
  }

  def withAuth(accessToken: AccessToken) = {
    val newResourceClient = resourceClient(accessToken.guid.toString)
    new DefaultAccessTokenClient(newResourceClient)
  }

}

trait AccessTokenClient {

  def getAccessToken(guid: UUID): Future[Option[AccessToken]]

  def createAdminAccessTokenForAccount(accountGuid: UUID): Future[Either[String, AuthorizedAccessToken]]

  def getAccessTokens(): Future[Seq[AuthorizedAccessToken]]

  def createAccessToken(clientGuid: UUID, scopes: Seq[Scope]): Future[Either[String, AuthorizedAccessToken]]

}
