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
import services.budget.TransfersService
import models.budget.JsonImplicits._

object TransfersController extends Controller with PaginationController {

  val transfersService = TransfersService.default()

  case class CreateData(
    fromAccountGuid: UUID,
    toAccountGuid: UUID,
    amount: BigDecimal,
    timestamp: DateTime
  )
  private implicit val cdr = Json.reads[CreateData]

  def create = Auth.async(parse.json) { request =>
    request.body.validate[CreateData].fold(
      error => Future.successful(Conflict(error.toString)),
      data => transfersService.createTransfer(
        fromAccountGuid = data.fromAccountGuid,
        toAccountGuid = data.toAccountGuid,
        amount = data.amount,
        timestamp = data.timestamp,
        accessToken = request.accessToken
      ).flatMap { result =>
        result.fold(
          errors => Future.successful(Conflict(Json.toJson(errors))),
          transferRecord => {
            transfersService.records2models(Seq(transferRecord)).map { transfers =>
              Created(Json.toJson(transfers.head))
            }
          }
        )
      }
    )
  }

}
