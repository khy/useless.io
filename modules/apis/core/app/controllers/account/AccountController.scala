package controllers.core.account

import java.util.UUID
import scala.concurrent.Future
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._
import io.useless.play.json.account.AccountJson._

import controllers.core.auth.Auth
import models.core.account.{ Account, Scope }

object AccountController extends Controller {

  def get(guid: UUID) = Auth.async { request =>
    Account.forGuid(guid).map { optAccount =>
      optAccount.map { account =>
        val isAuthRequest = request.accessToken.scopes.contains(Scope.Auth)
        val isAdminRequest = request.accessToken.scopes.contains(Scope.Admin)
        val isRequestorsAccount = request.accessToken.resourceOwner.guid == account.guid

        if (isAuthRequest || (isAdminRequest && isRequestorsAccount)) {
          Ok(Json.toJson(account.toAuthorized))
        } else {
          Ok(Json.toJson(account.toPublic))
        }
      }.getOrElse {
        NotFound
      }
    }
  }

  def find = Auth.async { request =>
    val optEmail = request.queryString.get("email").flatMap(_.headOption)
    val optHandle = request.queryString.get("handle").flatMap(_.headOption)

    def accountResponse(optAccount: Option[Account]) = {
      val accounts = optAccount.map(_.toPublic).toSeq
      Ok(Json.toJson(accounts))
    }

    if (optEmail.isDefined) {
      Account.forEmail(optEmail.get).map(accountResponse(_))
    } else if (optHandle.isDefined) {
      Account.forHandle(optHandle.get).map(accountResponse(_))
    } else {
      Future.successful {
        UnprocessableEntity("email or handle must be specified.")
      }
    }
  }

}
