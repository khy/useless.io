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
import io.useless.play.json.MessageJson.format
import io.useless.play.pagination.PaginationController

import controllers.budget.auth.Auth
import services.budget.TransactionsService
import models.budget.JsonImplicits._
import util.FormFormats._

object TransactionsController extends Controller with PaginationController {

  val transactionsService = TransactionsService.default()

  case class IndexQuery(
    accountGuid: Option[UUID]
  )

  val indexQueryForm = Form(
    mapping(
      "accountGuid" -> optional(uuid)
    )(IndexQuery.apply)(IndexQuery.unapply)
  )

  def index = Auth.async { implicit request =>
    indexQueryForm.bindFromRequest.fold(
      formWithErrors => Future.successful(Conflict(formWithErrors.errorsAsJson)),
      indexQuery => withRawPaginationParams { rawPaginationParams =>
        transactionsService.findTransactions(
          accountGuids = indexQuery.accountGuid.map(Seq(_)),
          userGuids = Some(Seq(request.accessToken.resourceOwner.guid)),
          rawPaginationParams = rawPaginationParams
        ).flatMap { result =>
          result.fold(
            errors => Future.successful(Conflict(Json.toJson(errors))),
            result => {
              transactionsService.records2models(result.items).map { transactions =>
                val _result = result.copy(items = transactions)
                paginatedResult(routes.TransactionsController.index, _result)
              }
            }
          )
        }
      }
    )
  }

  case class CreateData(
    transactionTypeGuid: UUID,
    accountGuid: UUID,
    amount: BigDecimal,
    date: LocalDate,
    name: Option[String],
    plannedTransactionGuid: Option[UUID]
  )
  private implicit val cdr = Json.reads[CreateData]

  def create = Auth.async(parse.json) { request =>
    request.body.validate[CreateData].fold(
      error => Future.successful(Conflict(error.toString)),
      data => transactionsService.createTransaction(
        transactionTypeGuid = data.transactionTypeGuid,
        accountGuid = data.accountGuid,
        amount = data.amount,
        date = data.date,
        name = data.name,
        plannedTransactionGuid = data.plannedTransactionGuid,
        adjustedTransactionGuid = None,
        accessToken = request.accessToken
      ).flatMap { result =>
        result.fold(
          errors => Future.successful(Conflict(Json.toJson(errors))),
          transactionRecord => {
            transactionsService.records2models(Seq(transactionRecord)).map { transactions =>
              Created(Json.toJson(transactions.head))
            }
          }
        )
      }
    )
  }

  def delete(guid: UUID) = Auth.async { request =>
    transactionsService.deleteTransaction(
      transactionGuid = guid,
      accessToken = request.accessToken
    ).map { result =>
      result.fold(
        errors => Conflict(Json.toJson(errors)),
        _ => NoContent
      )
    }
  }

  case class AdjustData(
    transactionTypeGuid: Option[UUID],
    accountGuid: Option[UUID],
    amount: Option[BigDecimal],
    date: Option[LocalDate],
    name: Option[String]
  )
  private implicit val adr = Json.reads[AdjustData]

  def adjust(transactionGuid: UUID) = Auth.async(parse.json) { request =>
    request.body.validate[AdjustData].fold(
      error => Future.successful(Conflict(error.toString)),
      data => transactionsService.adjustTransaction(
        transactionGuid = transactionGuid,
        trasanctionTypeGuid = data.transactionTypeGuid,
        accountGuid = data.accountGuid,
        amount = data.amount,
        date = data.date,
        name = data.name,
        accessToken = request.accessToken
      ).flatMap { result =>
        result.fold(
          errors => Future.successful(Conflict(Json.toJson(errors))),
          adjustedTransactionRecord => {
            transactionsService.records2models(Seq(adjustedTransactionRecord)).map { adjustedTransactions =>
              Created(Json.toJson(adjustedTransactions.head))
            }
          }
        )
      }
    )
  }

}
