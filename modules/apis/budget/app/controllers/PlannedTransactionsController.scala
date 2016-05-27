package controllers.budget

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data._
import play.api.data.Forms._
import org.joda.time.LocalDate
import io.useless.play.json.DateTimeJson._
import io.useless.play.json.validation.ErrorsJson._
import io.useless.play.pagination.PaginationController

import controllers.budget.auth.Auth
import services.budget.PlannedTransactionsService
import models.budget.JsonImplicits._
import util.FormFormats._

object PlannedTransactionsController extends Controller with PaginationController {

  val plannedTransactionsService = PlannedTransactionsService.default()

  case class IndexQuery(
    guid: Option[UUID],
    context: Option[UUID],
    account: Option[UUID],
    transactionType: Option[UUID],
    minDateFrom: Option[LocalDate],
    minDateTo: Option[LocalDate],
    maxDateFrom: Option[LocalDate],
    maxDateTo: Option[LocalDate],
    transactionCountFrom: Option[Int],
    transactionCountTo: Option[Int]
  )

  val indexQueryForm = Form(
    mapping(
      "guid" -> optional(uuid),
      "context" -> optional(uuid),
      "account" -> optional(uuid),
      "transactionType" -> optional(uuid),
      "minDateFrom" -> optional(jodaLocalDate),
      "minDateTo" -> optional(jodaLocalDate),
      "maxDateFrom" -> optional(jodaLocalDate),
      "maxDateTo" -> optional(jodaLocalDate),
      "transactionCountFrom" -> optional(number),
      "transactionCountTo" -> optional(number)
    )(IndexQuery.apply)(IndexQuery.unapply)
  )

  def index = Auth.async { implicit request =>
    indexQueryForm.bindFromRequest.fold(
      formWithErrors => Future.successful(Conflict(formWithErrors.errorsAsJson)),
      indexQuery => withRawPaginationParams { rawPaginationParams =>
        withRawPaginationParams { rawPaginationParams =>
          plannedTransactionsService.findPlannedTransactions(
            guids = indexQuery.guid.map(Seq(_)),
            contextGuids = indexQuery.context.map(Seq(_)),
            accountGuids = indexQuery.account.map(Seq(_)),
            transactionTypeGuids = indexQuery.transactionType.map(Seq(_)),
            minDateFrom = indexQuery.minDateFrom,
            minDateTo = indexQuery.minDateTo,
            maxDateFrom = indexQuery.maxDateFrom,
            maxDateTo = indexQuery.maxDateTo,
            transactionCountFrom = indexQuery.transactionCountFrom,
            transactionCountTo = indexQuery.transactionCountTo,
            userGuids = Some(Seq(request.accessToken.resourceOwner.guid)),
            rawPaginationParams = rawPaginationParams
          ).map { result =>
            result.fold(
              errors => Conflict(Json.toJson(errors)),
              plannedTransactions => paginatedResult(routes.PlannedTransactionsController.index, plannedTransactions)
            )
          }
        }
      }
    )
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
