package controllers.budget

import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json

import controllers.budget.auth.Auth
import models.budget.TransactionClass
import models.budget.util.NamedEnumJson

object TransactionClassesController extends Controller {

  implicit val transactionClassWrites = NamedEnumJson.fullWrites[TransactionClass]

  def index = Auth { implicit request =>
    Ok(Json.toJson(TransactionClass.values))
  }

}
