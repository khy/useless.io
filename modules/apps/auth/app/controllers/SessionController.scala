package controllers.auth

import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import services.auth.LoginService

object SessionController extends Controller {

  case class SignInData(email: String, password: String)

  val signInForm = Form {
    mapping(
      "email" -> email,
      "password" -> nonEmptyText
    )(SignInData.apply)(SignInData.unapply)
  }

  def form = Action {
    Ok(views.html.auth.session.form(signInForm))
  }

  def create = Action.async(parse.urlFormEncoded) { implicit request =>
    signInForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(UnprocessableEntity(views.html.auth.session.form(formWithErrors)))
      },
      signInData => {
        LoginService.auth(signInData.email, signInData.password).map { optAccessToken =>
          optAccessToken.map { accessToken =>
            Redirect(request.cookies.get("return_path").map(_.value).getOrElse("/")).
              withSession("auth" -> accessToken.guid.toString).
              discardingCookies(DiscardingCookie("return_path"))
          }.getOrElse {
            val formWithError = signInForm.withGlobalError("error.invalid-sign-in")
            Unauthorized(views.html.auth.session.form(formWithError))
          }
        }
      }
    )
  }

  def delete = Action { implicit request =>
    Redirect(routes.SessionController.form).withSession(request.session - "auth")
  }

}
