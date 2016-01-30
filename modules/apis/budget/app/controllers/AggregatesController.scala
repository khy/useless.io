package controllers.budget

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.joda.time.LocalDate
import io.useless.play.json.MessageJson.format

import models.budget.JsonImplicits._
import controllers.budget.auth.Auth
import services.budget.aggregates.TransactionTypeRollupsService

object AggregatesController extends Controller {

  val transactionTypeRollupsService = TransactionTypeRollupsService.default()

  def transactionTypeRollups = Auth.async { implicit request =>
    transactionTypeRollupsService.getTransactionTypeRollups(
      fromDate = request.queryString.get("fromDate").
        flatMap(_.headOption).
        map { raw => new LocalDate(raw) },
      toDate = request.queryString.get("toDate").
        flatMap(_.headOption).
        map { raw => new LocalDate(raw) },
      accessToken = request.accessToken
    ).map { result =>
      result.fold(
        errors => Conflict(Json.toJson(errors)),
        rollups => Ok(Json.toJson(rollups))
      )
    }
  }

}
