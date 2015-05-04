package controllers.auth

import play.api._
import play.api.mvc._
import io.useless.account.User

import controllers.auth.util.authentication.Authenticated

object MainController extends Controller {

  def index = Authenticated { request =>
    val user = request.accessToken.resourceOwner.asInstanceOf[User]
    val userDisplay = user.name.getOrElse(user.handle)
    Ok(views.html.auth.index(userDisplay))
  }

}
