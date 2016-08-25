package clients.auth.accesstoken

import java.util.UUID
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.ws.WS

import io.useless.play.client.ResourceClient
import io.useless.accesstoken.{ AccessToken, AuthorizedAccessToken, Scope }
import io.useless.util.configuration.Configuration

object AccessTokenClient
  extends DefaultAccessTokenClientComponent
  with    Configuration
{

  def instance(optAccessToken: Option[AccessToken] = None): AccessTokenClient = {
    val baseUrl = configuration.underlying.getString("useless.core.baseUrl")
    val auth = optAccessToken.map(_.guid.toString).getOrElse {
      configuration.underlying.getString("account.accessTokenGuid")
    }
    val resourceClient = ResourceClient(WS.client, baseUrl, auth)
    new DefaultAccessTokenClient(resourceClient)
  }

}

trait AccessTokenClient {

  def getAccessToken(guid: UUID): Future[Option[AccessToken]]

  def createAdminAccessTokenForAccount(accountGuid: UUID): Future[Either[String, AuthorizedAccessToken]]

  def getAccessTokens(): Future[Seq[AuthorizedAccessToken]]

  def createAccessToken(clientGuid: UUID, scopes: Seq[Scope]): Future[Either[String, AuthorizedAccessToken]]

}
