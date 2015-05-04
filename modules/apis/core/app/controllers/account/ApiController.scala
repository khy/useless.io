package controllers.core.account

import scala.concurrent.Future
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._
import io.useless.play.json.account.AccountJson._

import controllers.core.auth.Auth
import models.core.account.{ Account, Scope }

object ApiController extends Controller {

  def create = Auth.scope(Scope.Platform).async(parse.json) { request =>
    (request.body \ "key").asOpt[String].map { key =>
      Account.createApi(key).map { account =>
        account match {
          case Left(msg: String) => UnprocessableEntity(msg)
          case Right(account: Account) => Created(Json.toJson(account.toPublic))
        }
      }
    }.getOrElse {
      Future.successful(UnprocessableEntity("Key must be specified."))
    }
  }

}
