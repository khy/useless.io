package controllers.budget

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.joda.time.LocalDate
import io.useless.play.json.DateTimeJson._
import io.useless.play.json.MessageJson.format
import io.useless.play.pagination.PaginationController

import controllers.budget.auth.Auth
import services.budget.PlannedTransactionsService
import models.budget.JsonImplicits._

object PlannedTransactionsController extends Controller with PaginationController {

  val plannedTransactionsService = PlannedTransactionsService.default()

  def index = Auth.async { implicit request =>
    withRawPaginationParams { rawPaginationParams =>
      plannedTransactionsService.findPlannedTransactions(
        createdByAccounts = Some(Seq(request.accessToken.resourceOwner.guid)),
        rawPaginationParams = rawPaginationParams
      ).map { result =>
        result.fold(
          errors => Conflict(Json.toJson(errors)),
          plannedTransactions => paginatedResult(routes.PlannedTransactionsController.index, plannedTransactions)
        )
      }
    }
  }

  case class CreateData(
    transactionTypeGuid: UUID,
    accountGuid: UUID,
    minAmount: Option[BigDecimal],
    maxAmount: Option[BigDecimal],
    minDate: Option[LocalDate],
    maxDate: Option[LocalDate],
    name: Option[String]
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
        minDate = data.minDate,
        maxDate = data.maxDate,
        name = data.name,
        accessToken = request.accessToken
      ).map { result =>
        result.fold(
          errors => Conflict(Json.toJson(errors)),
          plannedTransaction => Created(Json.toJson(plannedTransaction))
        )
      }
    )
  }

  def delete(guid: UUID) = Auth.async { request =>
    plannedTransactionsService.deletePlannedTransaction(
      plannedTransactionGuid = guid,
      accessToken = request.accessToken
    ).map { result =>
      result.fold(
        errors => Conflict(Json.toJson(errors)),
        _ => NoContent
      )
    }
  }

}
