package controllers.auth.util.authentication

import java.util.UUID
import play.api.Play
import play.api.Play.current
import play.api.mvc.{ Cookie, Request, Results }
import play.api.libs.ws.WS
import io.useless.play.authentication._
import io.useless.client.accesstoken.{AccessTokenClient, AccessTokenClientComponents}
import io.useless.util.configuration.RichConfiguration._

import controllers.auth.routes.SessionController

object Authenticated
  extends BaseAuthenticated
  with    SignInRejectorComponent
  with    AccessTokenClientComponents
{

  val accessTokenClient = AccessTokenClient.instance(
    client = WS.client,
    baseUrl = Play.configuration.underlying.getString("useless.core.baseUrl"),
    authGuid = Play.configuration.underlying.getUuid("account.accessTokenGuid")
  )

  override lazy val authenticator = new SessionAuthenticator(accessTokenClient, "auth")

  override val rejector = new SignInRejector

}

class SessionAuthenticator(
  accessTokenClient: AccessTokenClient,
  key: String
) extends GuidAuthenticator(accessTokenClient) {

  def guid[A](request: Request[A]) = request.session.get(key)

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
