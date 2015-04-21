package clients.auth.accesstoken

import java.util.UUID
import scala.concurrent.Future
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import io.useless.accesstoken.{ AccessToken, AuthorizedAccessToken, Scope }
import io.useless.play.client.ResourceClientComponent
import io.useless.play.json.UuidJson._
import io.useless.play.json.accesstoken.AccessTokenJson._

import clients.auth.UnexpectedAccessTokenTypeException

trait DefaultAccessTokenClientComponent {

  self: ResourceClientComponent =>

  class DefaultAccessTokenClient(
    resourceClient: ResourceClient
  ) extends AccessTokenClient {

    def getAccessToken(guid: UUID) = {
      resourceClient.get[AccessToken](s"/access_tokens/$guid")
    }

    def createAdminAccessTokenForAccount(accountGuid: UUID) = {
      resourceClient.create[AccessToken](s"/accounts/$accountGuid/access_tokens", Json.obj()).map { result =>
        result.right.map { accessToken =>
          accessToken match {
            case authorizedAccessToken: AuthorizedAccessToken => authorizedAccessToken
            case accessToken => throw new UnexpectedAccessTokenTypeException(accessToken, "AuthorizedAccessToken")
          }
        }
      }
    }

    def getAccessTokens(): Future[Seq[AuthorizedAccessToken]] = {
      resourceClient.find[AccessToken]("/access_tokens").map { result =>
        result.items.map { accessToken =>
          accessToken match {
            case authorizedAccessToken: AuthorizedAccessToken => authorizedAccessToken
            case accessToken => throw new UnexpectedAccessTokenTypeException(accessToken, "AuthorizedAccessToken")
          }
        }
      }
    }

    def createAccessToken(clientGuid: UUID, scopes: Seq[Scope]): Future[Either[String, AuthorizedAccessToken]] = {
      val body = Json.obj("client_guid" -> clientGuid, "scopes" -> scopes.map(_.toString))
      resourceClient.create[AccessToken]("/access_tokens", body).map { result =>
        result.right.map { accessToken =>
          accessToken match {
            case authorizedAccessToken: AuthorizedAccessToken => authorizedAccessToken
            case accessToken => throw new UnexpectedAccessTokenTypeException(accessToken, "AuthorizedAccessToken")
          }
        }
      }
    }

  }

}
