package controllers.core.accesstoken

import java.util.UUID
import scala.concurrent.Future
import play.api.mvc._
import play.api.libs.json.Json
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import org.joda.time.DateTime
import io.useless.play.json.UuidJson._
import io.useless.play.json.accesstoken.AccessTokenJson._

import controllers.core.auth.Auth
import models.core.account.{ Account, AccessToken, Scope }

object AuthorizationController extends Controller {

  val InvalidResponse = Future.successful(UnprocessableEntity("Invalid account_guid / authorization_code combination"))

  def authorize(authorizationCode: UUID) = Auth.async { request =>
    Account.forAuthorizationCode(authorizationCode).flatMap { optAccount =>
      optAccount.map { account =>
        val accessToken = account.accessTokens.find { accessToken =>
          accessToken.authorizationCode == authorizationCode
        }.get

        if (accessToken.clientGuid == Some(request.accessToken.resourceOwner.guid)) {
          if (accessToken.isDeleted) {
            Logger.debug(s"Access token [${accessToken.guid}] has been deleted.")
            InvalidResponse
          } else if (accessToken.isAuthorized) {
            accessToken.delete()
            Logger.debug(s"Access token [${accessToken.guid}] has already been authorized (by code [${authorizationCode}] at [${accessToken.authorizedAt.get}]) - deleting.")
            InvalidResponse
          } else {
            if (accessToken.createdAt.isBefore(DateTime.now.minusMinutes(10))) {
              accessToken.delete()
              Logger.debug(s"Authorization window for access token [${accessToken.guid}] has expired (created at [${accessToken.createdAt}]) - deleting.")
              InvalidResponse
            } else {
              accessToken.authorize()
              accessToken.toPublic.map { publicAccessToken =>
                Created(Json.toJson(publicAccessToken))
              }
            }
          }
        } else {
          Logger.debug(s"Access token [${accessToken.guid}] does not belong to the authenticated access token [${request.accessToken.guid}].")
          InvalidResponse
        }
      }.getOrElse {
        Logger.debug(s"Authorization code [${authorizationCode}] does not exist.")
        InvalidResponse
      }
    }
  }

}
