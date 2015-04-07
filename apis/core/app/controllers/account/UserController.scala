package controllers.core.account

import scala.concurrent.Future
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._
import io.useless.play.json.account.AccountJson._

import models.core.account.Account

object UserController extends Controller {

  def create = Action.async(parse.json) { request =>
    val maybeEmail = (request.body \ "email").asOpt[String]
    val maybeHandle = (request.body \ "handle").asOpt[String]
    val maybeName = (request.body \ "name").asOpt[String]

    if (maybeEmail.isEmpty) {
      Future.successful(UnprocessableEntity("email must be specified"))
    } else if (maybeHandle.isEmpty) {
      Future.successful(UnprocessableEntity("handle must be specified"))
    } else {
      val futureAccount = Account.createUser(
        maybeEmail.get,
        maybeHandle.get,
        maybeName
      )

      futureAccount.map { account =>
        account match {
          case Left(msg: String) => UnprocessableEntity(msg)
          case Right(account: Account) => Created(Json.toJson(account.toPublic))
        }
      }
    }
  }

}
