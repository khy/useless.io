package controllers.budget

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.joda.time.DateTime
import io.useless.play.json.DateTimeJson._
import io.useless.play.json.MessageJson.format
import io.useless.play.pagination.PaginationController

import controllers.budget.auth.Auth
import services.budget.PlannedTransactionsService
import models.budget.JsonImplicits._

object PlannedTransactionsController extends Controller with PaginationController {

  val plannedTransactionsService = PlannedTransactionsService.default()

  case class CreateData(
    transactionTypeGuid: UUID,
    accountGuid: UUID,
    minAmount: Option[BigDecimal],
    maxAmount: Option[BigDecimal],
    minTimestamp: Option[DateTime],
    maxTimestamp: Option[DateTime]
  )
  private implicit val cdr = Json.reads[CreateData]

  def create = Auth.async(parse.json) { request =>
    request.body.validate[CreateData].fold(
      error => Future.successful(Conflict(error.toString)),
      data => plannedTransactionsService.createPlannedTransaction(
        transactionTypeGuid = data.transactionTypeGuid,
        accountGuid = data.accountGuid,
        minAmount = data.minAmount,
        maxAmount = data.maxAmount,
        minTimestamp = data.minTimestamp,
        maxTimestamp = data.maxTimestamp,
        accessToken = request.accessToken
      ).map { result =>
        result.fold(
          errors => Conflict(Json.toJson(errors)),
          plannedTransaction => Created(Json.toJson(plannedTransaction))
        )
      }
    )
  }

}
