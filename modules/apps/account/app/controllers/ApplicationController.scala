package controllers.account

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current
import io.useless.account.User

import controllers.account.authentication.Auth
import controllers.account.authentication.SessionAuthenticatorComponent

object ApplicationController
  extends Controller
{

  def index = Action.async { request =>
    Auth.authenticator.authenticate(request).map { optAccessToken =>
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
