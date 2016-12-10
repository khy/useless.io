package controllers.budget

import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import io.useless.play.json.NamedEnumJson

import controllers.budget.auth.Auth
import models.budget.AccountType

object AccountTypesController extends Controller {

  implicit val accountTypeWrites = NamedEnumJson.fullWrites[AccountType]

  def index = Auth { implicit request =>
    Ok(Json.toJson(AccountType.values))
  }

}
