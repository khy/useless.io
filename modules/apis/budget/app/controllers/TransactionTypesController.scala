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
import services.budget.TransactionTypesService
import models.budget.TransactionClass
import models.budget.JsonImplicits._

object TransactionTypesController extends Controller {

  val transactionTypesService = TransactionTypesService.default()

  case class CreateData(
    transactionClass: TransactionClass,
    accountGuid: UUID,
    name: String
  )
  private implicit val cdr = Json.reads[CreateData]

  def create = Auth.async(parse.json) { request =>
    request.body.validate[CreateData].fold(
      error => Future.successful(Conflict(error.toString)),
      data => transactionTypesService.createTransactionType(
        transactionClass = data.transactionClass,
        accountGuid = data.accountGuid,
        name = data.name,
        accessToken = request.accessToken
      ).map { result =>
        result.fold(
          errors => Conflict(Json.toJson(errors)),
          transactionType => Created(Json.toJson(transactionType))
        )
      }
    )
  }

}