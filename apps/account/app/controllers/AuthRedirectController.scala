package controllers.account

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import daos.account.AuthorizationDao
import clients.account.AuthorizationClient

object AuthRedirectController extends Controller {

  val authorizationClient = AuthorizationClient.instance
  val authorizationDao = AuthorizationDao.instance

  def auth(code: String) = Action.async { request =>
    val authorizationCode = UUID.fromString(code)

    authorizationDao.forAuthorizationCode(authorizationCode).flatMap { optAuthorizationDocument =>
      optAuthorizationDocument.map { authorizationDocument =>
        Future.successful(Right(authorizationDocument.accessTokenGuid))
      }.getOrElse {
        authorizationClient.authorize(authorizationCode).flatMap { result =>
          result.fold(
            error => Future.successful(Left(error)),
            accessToken => {
              authorizationDao.create(authorizationCode, accessToken.guid).map { result =>
                result.fold(
                  error => Left(error),
                  authorizationDocument => Right(authorizationDocument.accessTokenGuid)
                )
              }
            }
          )
        }
      }
    }.map { result =>
      result.fold(
        error => UnprocessableEntity(error),
        accessTokenGuid => {
          Redirect(request.cookies.get("return_path").map(_.value).getOrElse("/")).
            withSession("auth" -> accessTokenGuid.toString).
            discardingCookies(DiscardingCookie("return_path"))
        }
      )
    }
  }

}
