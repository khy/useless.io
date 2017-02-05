package controllers.core.account

import java.util.UUID
import scala.concurrent.Future
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._
import io.useless.play.http.QueryStringUtil._
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
    val optGuids = request.richQueryString.get[UUID]("guid")
    val optEmail = request.queryString.get("email").flatMap(_.headOption)
    val optHandle = request.queryString.get("handle").flatMap(_.headOption)

    def accountResponse(accounts: Seq[Account]) = {
      Ok(Json.toJson(accounts.map(_.toPublic)))
    }

    if (optGuids.isDefined) {
      Account.forGuids(optGuids.get).map(accountResponse)
    } else if (optEmail.isDefined) {
      Account.forEmail(optEmail.get).map { optAccount => accountResponse(optAccount.toSeq) }
    } else if (optHandle.isDefined) {
      Account.forHandle(optHandle.get).map { optAccount => accountResponse(optAccount.toSeq) }
    } else {
      Future.successful {
        UnprocessableEntity("guid, email or handle must be specified.")
      }
    }
  }

}
