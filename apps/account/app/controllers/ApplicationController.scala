package controllers.account

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.account.User
import io.useless.play.authentication.ClientAuthDaoComponent

import controllers.account.authentication.SessionAuthenticatorComponent

object ApplicationController
  extends Controller
  with    ClientAuthDaoComponent
  with    SessionAuthenticatorComponent
{

  val authDao = new ClientAuthDao
  val authenticator = new SessionAuthenticator("auth")

  def index = Action.async { request =>
    authenticator.authenticate(request).map { optAccessToken =>
      optAccessToken.map { accessToken =>
        Redirect(routes.AccountController.index)
      }.getOrElse {
        Ok(views.html.account.index())
      }
    }

  }

  def signOut = Action { implicit request =>
    Redirect(routes.ApplicationController.index).withSession(request.session - "auth")
  }

}
