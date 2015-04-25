package controllers.account

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import io.useless.account.{ User, AuthorizedUser }
import io.useless.play.authentication.AuthenticatedRequest

import controllers.account.authentication.Auth
import clients.account.AccountClient

object AccountController extends Controller {

  def index = Auth.async { implicit request =>
    val accountClient = AccountClient.instance(request.accessToken)
    accountClient.getUser(request.accessToken.resourceOwner.guid).map { optUser =>
      optUser.map { user =>
        Ok(views.html.account.account.index(user))
      }.getOrElse {
        throw new RuntimeException("Could not retrieve AuthorizedUser")
      }
    }
  }

}
