package controllers.auth.util.authentication

import java.util.UUID
import play.api.mvc.{ Cookie, Request, Results }
import io.useless.play.authentication._

import controllers.auth.routes.SessionController
import clients.auth.accesstoken.AccessTokenClient

object Authenticated
  extends BaseAuthenticated
  with    ApplicationClientAuthDaoComponent
  with    SessionAuthenticatorComponent
  with    SignInRejectorComponent
{

  val authDao = new ApplicationClientAuthDao

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
      Results.Redirect(SessionController.form).
        withCookies(Cookie("return_path", returnPath))
    }

  }

}

/*
 * In order to ensure proper mocking, use the application's AccessTokenClient
 * as opposed to useless.scala's.
 */
trait ApplicationClientAuthDaoComponent extends AuthDaoComponent {

  class ApplicationClientAuthDao extends AuthDao {

    private lazy val client = AccessTokenClient.instance

    def getAccessToken(guid: UUID) = client.getAccessToken(guid)

  }

}
