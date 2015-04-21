package controllers.auth

import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import services.auth.LoginService

object LoginController extends Controller {

  case class SignUpData(name: String, email: String, handle: String, password: String)

  val signUpForm = Form {
    mapping(
      "name" -> nonEmptyText,
      "email" -> email,
      "handle" -> nonEmptyText,
      "password" -> nonEmptyText
    )(SignUpData.apply)(SignUpData.unapply)
  }

  def form = Action {
    Ok(views.html.auth.login.form(signUpForm))
  }

  def create = Action.async { implicit request =>
    signUpForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(UnprocessableEntity(views.html.auth.login.form(formWithErrors)))
      },
      signUpData => {
        LoginService.create(
          signUpData.email,
          signUpData.password,
          Some(signUpData.handle),
          Some(signUpData.name)
        ).map { result =>
          result.fold(
            error => {
              val formWithError = signUpForm.withGlobalError(error)
              UnprocessableEntity(views.html.auth.login.form(formWithError))
            },
            accessToken => {
              Redirect(request.cookies.get("return_path").map(_.value).getOrElse("/")).
                withSession("auth" -> accessToken.guid.toString).
                discardingCookies(DiscardingCookie("return_path"))
            }
          )
        }
      }
    )
  }

}
