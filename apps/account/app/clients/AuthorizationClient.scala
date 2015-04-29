package clients.account

import java.util.UUID
import scala.concurrent.Future
import play.api.Play
import play.api.libs.json.Json
import io.useless.play.client.{ ResourceClient, ResourceClientComponent }
import io.useless.accesstoken.AccessToken
import io.useless.play.json.accesstoken.AccessTokenJson._

object AuthorizationClient
  extends DefaultAuthorizationClientComponent
  with ResourceClient
{

  lazy val instance = {
    val config = Play.current.configuration
    val accessTokenGuid = config.getString("account.accessTokenGuid").get
    val _resourceClient = resourceClient(accessTokenGuid)
    new DefaultAuthorizationClient(_resourceClient)
  }

}

trait DefaultAuthorizationClientComponent {

  self: ResourceClientComponent =>

  class DefaultAuthorizationClient(resourceClient: ResourceClient) {

    def authorize(authorizationCode: UUID) = {
      val path = s"/access_tokens/authorizations/$authorizationCode"
      resourceClient.create[AccessToken](path, Json.obj())
    }

  }

}
