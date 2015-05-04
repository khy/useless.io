package controllers.core.accesstoken

import java.util.UUID
import scala.concurrent.Future
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._
import io.useless.accesstoken.{ Scope => UselessScope }
import io.useless.play.json.UuidJson._
import io.useless.play.json.accesstoken.AccessTokenJson._

import controllers.core.auth.Auth
import models.core.account.{ Account, AccessToken, Scope }

object AccessTokenController extends Controller {

  /*
   * Returns the public version of an access token corresponding to the
   * specified GUID.
   *
   * Requires authentication, but no particular scope.
   */
  def get(guid: UUID) = Auth.async { request =>
    Account.forAccessToken(guid).flatMap { maybeAccount =>
      maybeAccount.flatMap { account =>
        account.accessTokens.find { accessToken =>
          accessToken.guid == guid && !accessToken.isDeleted
        }.map { accessToken =>
          accessToken.toPublic.map { publicAccessToken =>
            Ok(Json.toJson(publicAccessToken))
          }
        }
      }.getOrElse {
        Future.successful(NotFound)
      }
    }
  }

  /*
   * Returns the authorized versions of all of the access tokens belonging to
   * the resource owner of the authenticated access token.
   *
   * Requires Admin scope.
   */
  def find = Auth.scope(Scope.Admin).async { request =>
    val accountGuid = request.accessToken.resourceOwner.guid

    Account.forGuid(accountGuid).flatMap { maybeAccount =>
      maybeAccount.map { account =>
        val accessTokens = account.accessTokens.
          filter(!_.isDeleted).map(_.toAuthorized)

        Future.sequence(accessTokens).map { authorizedAccessTokens =>
          Ok(Json.toJson(authorizedAccessTokens))
        }
      }.getOrElse {
        Future.successful(NotFound)
      }
    }
  }

  /*
   * Creates a new access token for the resource owner of the authenticated
   * access token. Any client can be chosen for the new access token, via
   * 'client_guid' in the body. Any non-core scope may also be chosen.
   *
   * Requires Admin scope.
   */
  def create = Auth.scope(Scope.Admin).async(parse.json) { request =>
    val accountGuid = request.accessToken.resourceOwner.guid
    Account.forGuid(accountGuid).flatMap { maybeAccount =>
      maybeAccount.map { account =>
        val clientGuid = (request.body \ "client_guid").asOpt[String].map { rawGuid =>
          UUID.fromString(rawGuid)
        }

        val scopes = (request.body \ "scopes").asOpt[Seq[String]].map { scopes =>
          scopes.map(UselessScope(_))
        }.getOrElse(Seq.empty)

        val requestedRestrictedScopes = scopes.intersect(Scope.internal)
        if (requestedRestrictedScopes.length > 0) {
          Future.successful(UnprocessableEntity(s"The scopes ${requestedRestrictedScopes.mkString(", ")} cannot be requested"))
        } else {
          account.addAccessToken(clientGuid, scopes).flatMap { result =>
            result match {
              case Left(msg: String) => Future.successful(UnprocessableEntity(msg))
              case Right(accessToken: AccessToken) => accessToken.toAuthorized.map { authorizedAccessToken =>
                Created(Json.toJson(authorizedAccessToken))
              }
            }
          }
        }
      }.getOrElse {
        Future.successful(InternalServerError)
      }
    }
  }

  /*
   * Creates a new access token for which the specified GUID is resource
   * owner and the authenticated access token's resource owner is client. Any
   * requested, non-core scopes will be added to the new access token. In
   * addition, if the requesting access token has Auth scope, the Admin scope
   * will also be added automatically.
   *
   * Or, from an account perspective, grants Admin access to the account making
   * the request, on behalf of the specified account.
   *
   * This is a *super power* within useless.io - requires Auth scope.
   */
  def createForAccount(guid: UUID) = Auth.scope(Scope.Auth, Scope.Trusted).async(parse.json) { request =>
    Account.forGuid(guid).flatMap { maybeAccount =>
      maybeAccount.map { account =>
        val clientGuid = request.accessToken.resourceOwner.guid

        var scopes = if (request.accessToken.scopes.contains(Scope.Trusted)) {
          (request.body \ "scopes").asOpt[Seq[String]].map { scopes =>
            scopes.map(UselessScope(_))
          }.getOrElse(Seq.empty)
        } else {
          Seq.empty
        }

        val requestedRestrictedScopes = scopes.intersect(Scope.core)
        if (!requestedRestrictedScopes.isEmpty) {
          Future.successful(UnprocessableEntity(s"The scopes ${requestedRestrictedScopes.mkString(", ")} cannot be requested"))
        } else {
          if (request.accessToken.scopes.contains(Scope.Auth)) {
            scopes = scopes ++ Seq(Scope.Admin)
          }

          account.addAccessToken(Some(clientGuid), scopes).flatMap { result =>
            result match {
              case Left(msg: String) => Future.successful(UnprocessableEntity(msg))
              case Right(accessToken: AccessToken) => accessToken.toAuthorized.map { authorizedAccessToken =>
                Created(Json.toJson(authorizedAccessToken))
              }
            }
          }
        }
      }.getOrElse {
        Future.successful(NotFound)
      }
    }
  }

}
