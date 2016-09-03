package controllers.core.auth

import java.util.UUID
import scala.concurrent.Future
import play.api.mvc.Controller
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import io.useless.accesstoken.Scope
import io.useless.play.authentication.{ BaseAuthenticated, AuthorizerComponent, ScopeAuthorizer }
import io.useless.client.accesstoken.{AccessTokenClient, AccessTokenClientComponents}

import models.core.account.Account

object Auth
  extends BaseAuthenticated
  with AccessTokenClientComponents
{

  val accessTokenClient = new ServerAccessTokenClient

  def scope(scopes: Scope*) = new ScopeAuth(scopes:_*)

}

private [auth] class ScopeAuth(scopes: Scope*)
  extends BaseAuthenticated
  with AuthorizerComponent
  with AccessTokenClientComponents
{

  val accessTokenClient = new ServerAccessTokenClient

  override val authorizer = new ScopeAuthorizer(scopes)

}

private [auth] class ServerAccessTokenClient extends AccessTokenClient {

  def getAccessToken(guid: UUID) = {
    Logger.debug(s"Attempting to authenticate access token [${guid}]")

    Account.forAccessToken(guid).flatMap { optAccount =>
      optAccount.flatMap { account =>
        account.accessTokens.find { accessToken =>
          accessToken.guid == guid
        }.map { accessToken =>
          accessToken.toPublic.map(Some(_))
        }
      }.getOrElse {
        Logger.debug(s"Could not find account for access token [${guid}]")
        Future.successful(None)
      }
    }
  }

}
