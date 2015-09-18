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
import services.budget.TransactionsService
import models.budget.JsonImplicits._

object TransactionsController extends Controller with PaginationController {

  val transactionsService = TransactionsService.default()

  def index = Auth.async { implicit request =>
    withRawPaginationParams { rawPaginationParams =>
      transactionsService.findTransactions(
        createdByAccounts = Some(Seq(request.accessToken.resourceOwner.guid)),
        rawPaginationParams = rawPaginationParams
      ).map { result =>
        result.fold(
          errors => Conflict(Json.toJson(errors)),
          transactions => paginatedResult(routes.TransactionsController.index, transactions)
        )
      }
    }
  }

  case class CreateData(
    transactionTypeGuid: UUID,
    amount: BigDecimal,
    timestamp: DateTime
  )
  private implicit val cdr = Json.reads[CreateData]

  def create = Auth.async(parse.json) { request =>
    request.body.validate[CreateData].fold(
      error => Future.successful(Conflict(error.toString)),
      data => transactionsService.createTransaction(
        transactionTypeGuid = data.transactionTypeGuid,
        amount = data.amount,
        timestamp = data.timestamp,
        accessToken = request.accessToken
      ).map { result =>
        result.fold(
          errors => Conflict(Json.toJson(errors)),
          transaction => Created(Json.toJson(transaction))
        )
      }
    )
  }

}
