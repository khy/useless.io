package clients.account

import java.util.UUID
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.json.Json
import io.useless.play.client.ResourceClient
import io.useless.accesstoken.AccessToken
import io.useless.play.json.accesstoken.AccessTokenJson._
import io.useless.util.configuration.Configuration

object AuthorizationClient
  extends DefaultAuthorizationClientComponent
  with    Configuration
{

  lazy val instance = {
    val baseUrl = configuration.underlying.getString("useless.core.baseUrl")
    val auth = configuration.underlying.getString("account.accessTokenGuid")
    val resourceClient = ResourceClient(baseUrl, auth)
    new DefaultAuthorizationClient(resourceClient)
  }

}

trait DefaultAuthorizationClientComponent {

  class DefaultAuthorizationClient(resourceClient: ResourceClient) {

    def authorize(authorizationCode: UUID) = {
      val path = s"/access_tokens/authorizations/$authorizationCode"
      resourceClient.create[AccessToken](path, Json.obj())
    }

  }

}
