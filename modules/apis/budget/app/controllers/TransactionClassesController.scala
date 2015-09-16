package controllers.budget

import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json

import controllers.budget.auth.Auth
import models.budget.TransactionClass
import models.budget.JsonImplicits._

object TransactionClassesController extends Controller {

  def index = Auth { implicit request =>
    Ok(Json.toJson(TransactionClass.values))
  }

}
