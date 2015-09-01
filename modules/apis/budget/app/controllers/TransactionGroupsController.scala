package controllers.budget

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.play.json.MessageJson.format

import controllers.budget.auth.Auth
import services.budget.TransactionGroupsService
import models.budget.TransactionType
import models.budget.JsonImplicits._

object TransactionGroupsController extends Controller {

  val transactionGroupsService = TransactionGroupsService.default()

  case class CreateData(
    transactionType: TransactionType,
    accountGuid: UUID,
    name: String
  )
  private implicit val cdr = Json.reads[CreateData]

  def create = Auth.async(parse.json) { request =>
    request.body.validate[CreateData].fold(
      error => Future.successful(Conflict(error.toString)),
      data => transactionGroupsService.createTransactionGroups(
        transactionType = data.transactionType,
        accountGuid = data.accountGuid,
        name = data.name,
        accessToken = request.accessToken
      ).map { result =>
        result.fold(
          errors => Conflict(Json.toJson(errors)),
          transactionGroup => Created(Json.toJson(transactionGroup))
        )
      }
    )
  }

}
