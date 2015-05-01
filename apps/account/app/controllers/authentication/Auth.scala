package controllers.account.authentication

import java.util.UUID
import play.api.mvc.{ Cookie, Request, Results }
import io.useless.play.authentication._

import controllers.account.routes.ApplicationController

object Auth
  extends BaseAuthenticated
  with    ClientAuthDaoComponent
  with    SessionAuthenticatorComponent
  with    SignInRejectorComponent
{

  val authDao = new ClientAuthDao("account.accessTokenGuid")

  override val authenticator = new SessionAuthenticator("auth")

  override val rejector = new SignInRejector

}

trait SessionAuthenticatorComponent extends GuidAuthenticatorComponent {

  self: AuthDaoComponent =>

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
