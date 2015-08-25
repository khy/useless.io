package controllers.budget

import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.joda.time.DateTime
import io.useless.play.json.MessageJson.format

import controllers.budget.auth.Auth
import services.budget.AccountsService
import models.budget.AccountType
import models.budget.JsonImplicits._

object AccountsController extends Controller {

  val accountsService = new AccountsService

  case class CreateData(accountType: AccountType, name: String)
  private implicit val cdr = Json.reads[CreateData]

  def create = Auth.async(parse.json) { request =>
    request.body.validate[CreateData].fold(
      error => Future.successful(Conflict),
      data => accountsService.createAccount(
        accountType = data.accountType,
        name = data.name
      ).map { result =>
        result.fold(
          errors => Conflict(Json.toJson(errors)),
          account => Created(Json.toJson(account))
        )
      }
    )
  }

}
