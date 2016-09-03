package controllers.account.authentication

import java.util.UUID
import play.api.mvc.{ Cookie, Request, Results }
import play.api.Play
import play.api.Play.current
import play.api.libs.ws.WS
import io.useless.play.authentication._
import io.useless.client.accesstoken.{AccessTokenClient, AccessTokenClientComponents}
import io.useless.util.configuration.RichConfiguration._

import controllers.account.routes.ApplicationController

object Auth
  extends BaseAuthenticated
  with    SessionAuthenticatorComponent
  with    SignInRejectorComponent
  with    AccessTokenClientComponents
{

  lazy val accessTokenClient = AccessTokenClient.instance(
    client = WS.client,
    baseUrl = Play.configuration.underlying.getString("useless.core.baseUrl"),
    authGuid = Play.configuration.underlying.getUuid("account.accessTokenGuid")
  )

  override val authenticator = new SessionAuthenticator("auth")

  override val rejector = new SignInRejector

}

trait SessionAuthenticatorComponent
  extends GuidAuthenticatorComponent
  with AccessTokenClientComponents
{

  class SessionAuthenticator(key: String) extends GuidAuthenticator {

    def guid[A](request: Request[A]) = request.session.get(key)

  }

}

trait SignInRejectorComponent extends RejectorComponent {

  class SignInRejector extends Rejector {

    def unauthenticated[A](request: Request[A]) = redirect(request)

    def unauthorized[A](request: Request[A]) = redirect(request)

    private def redirect[A](request: Request[A]) = {
      val returnPath = request.path + "?" + request.rawQueryString
      Results.Redirect(ApplicationController.index).
        withCookies(Cookie("return_path", returnPath))
    }

  }

}
