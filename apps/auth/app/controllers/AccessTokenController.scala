package controllers.auth

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.accesstoken.Scope
import io.useless.account.User

import clients.auth.accesstoken.AccessTokenClient
import clients.auth.account.AccountClient
import controllers.auth.util.authentication.Authenticated
import services.auth.ScopeService

object AccessTokenController extends Controller {

  lazy val accountClient = AccountClient.instance()
  lazy val scopeService = ScopeService.instance

  case class AuthData(appGuid: String, scopes: Option[String])

  val authForm = Form {
    mapping(
      "app_guid" -> nonEmptyText,
      "scopes" -> optional(text)
    )(AuthData.apply)(AuthData.unapply)
  }

  def form = Authenticated.async { implicit request =>
    val form = authForm.bindFromRequest

    if (form.hasErrors) {
      Logger.error(s"Access token form errors: ${form.errorsAsJson}")
      Future.successful(InternalServerError)
    } else {
      val accessTokenClient = AccessTokenClient.instance(Some(request.accessToken))

      val authData = form.value.get
      val appGuid = UUID.fromString(authData.appGuid)
      val scopes = parseScopes(authData.scopes)

      accessTokenClient.getAccessTokens().flatMap { accessTokens =>
        val optAccessToken = accessTokens.find { accessToken =>
          accessToken.client.map { _.guid == appGuid }.getOrElse(false) &&
          scopes.diff(accessToken.scopes).length == 0
        }

        accountClient.getApp(appGuid).map { optApp =>
          optApp.map { app =>
            optAccessToken.map { accessToken =>
              val redirectUrl = app.authRedirectUrl
              val authorizationCode = accessToken.authorizationCode
              Redirect(redirectUrl + "?code=" + authorizationCode)
            }.getOrElse {
              val user = request.accessToken.resourceOwner.asInstanceOf[User]
              val userDisplay = user.name.getOrElse(user.handle)
              Ok(views.html.auth.accesstoken.form(app, scopes, form, userDisplay))
            }
          }.getOrElse {
            UnprocessableEntity("Misconfigured")
          }
        }
      }
    }
  }

  def create = Authenticated.async(parse.urlFormEncoded) { implicit request =>
    authForm.bindFromRequest.fold(
      formWithErrors => {
        Logger.error(s"Access token form errors: ${formWithErrors.errorsAsJson}")
        Future.successful(InternalServerError)
      },
      authData => {
        request.body.get("action").flatMap(_.headOption).map { action =>
          val accessTokenClient = AccessTokenClient.instance(Some(request.accessToken))

          val appGuid = UUID.fromString(authData.appGuid)
          val scopes = parseScopes(authData.scopes)
          val futureApp = accountClient.getApp(appGuid)

          action match {
            case "Allow" => {
              accessTokenClient.createAccessToken(appGuid, scopes).flatMap { result =>
                result.fold(
                  error => {
                    Logger.error(s"Error when creating access token: $error")
                    Future.successful(InternalServerError)
                  },
                  accessToken => futureApp.map { optApp =>
                    optApp.map { app =>
                      val redirectUrl = app.authRedirectUrl
                      val authorizationCode = accessToken.authorizationCode
                      Redirect(redirectUrl + "?code=" + authorizationCode)
                    }.getOrElse {
                      Logger.error(s"Could not find app [$appGuid]")
                      InternalServerError
                    }
                  }
                )
              }
            }

            case "Deny" => futureApp.map { optApp =>
              optApp.map { app =>
                val redirectUrl = app.authRedirectUrl
                Redirect(redirectUrl + "?error=denied")
              }.getOrElse {
                Logger.error(s"Could not find app [$appGuid]")
                InternalServerError
              }
            }

            case action => {
              Logger.error(s"Invalid action: %action")
              Future.successful(InternalServerError)
            }
          }
        }.getOrElse {
          Future.successful(InternalServerError)
        }
      }
    )
  }

  private def parseScopes(optRawScopes: Option[String]) = optRawScopes.map { rawScopes =>
    if (rawScopes == "") {
      Seq.empty
    } else {
      rawScopes.split(",").map(scopeService.getRichScope(_)).toSeq
    }
  }.getOrElse(Seq.empty)

}
