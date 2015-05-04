package controllers.core.account

import scala.concurrent.Future
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._
import io.useless.play.json.account.AccountJson._

import controllers.core.auth.Auth
import models.core.account.{ Account, Scope }

object AppController extends Controller {

  def create = Auth.scope(Scope.Platform).async(parse.json) { request =>
    val optName = (request.body \ "name").asOpt[String]
    val optUrl = (request.body \ "url").asOpt[String]
    val optAuthRedirectUrl = (request.body \ "auth_redirect_url").asOpt[String]

    if (optName.isEmpty) {
      Future.successful(UnprocessableEntity("name must be specified"))
    } else if (optUrl.isEmpty) {
      Future.successful(UnprocessableEntity("url must be specified"))
    } else if (optAuthRedirectUrl.isEmpty) {
      Future.successful(UnprocessableEntity("auth_redirect_url must be specified"))
    } else {
      Account.createApp(optName.get, optUrl.get, optAuthRedirectUrl.get).map { account =>
        account match {
          case Left(msg: String) => UnprocessableEntity(msg)
          case Right(account: Account) => Created(Json.toJson(account.toPublic))
        }
      }
    }
  }

}
