package controllers.budget

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.joda.time.LocalDate
import io.useless.validation._
import io.useless.play.json.MessageJson.format
import io.useless.play.pagination.PaginationController

import models.budget.JsonImplicits._
import controllers.budget.auth.Auth
import services.budget.aggregates._

// TODO: Simple query string helpers.
object AggregatesController extends Controller with PaginationController {

  val projectionsService = ProjectionsService.default()

  def projections = Auth.async { implicit request =>
    val optDate = request.queryString.get("date").
      flatMap(_.headOption).
      map { raw => new LocalDate(raw) }

    optDate.map { date =>
      projectionsService.getProjections(
        date = date,
        userGuid = request.accessToken.resourceOwner.guid,
        accountGuids = request.queryString.get("accountGuid").
          map { rawGuids => rawGuids.map(UUID.fromString) }
      ).map { result =>
        result.fold(
          errors => Conflict(Json.toJson(errors)),
          projections => Ok(Json.toJson(projections))
        )
      }
    }.getOrElse {
      val failure = Validation.failure("date", "useless.error.missing")
      Future.successful(Conflict(Json.toJson(failure.toFailure.errors)))
    }
  }

  val transactionTypeRollupsService = TransactionTypeRollupsService.default()

  // TODO: Scope to a context.
  def transactionTypeRollups = Auth.async { implicit request =>
    transactionTypeRollupsService.getTransactionTypeRollups(
      fromDate = request.queryString.get("fromDate").
        flatMap(_.headOption).
        map { raw => new LocalDate(raw) },
      toDate = request.queryString.get("toDate").
        flatMap(_.headOption).
        map { raw => new LocalDate(raw) },
      userGuids = Some(Seq(request.accessToken.resourceOwner.guid))
    ).map { result =>
      result.fold(
        errors => Conflict(Json.toJson(errors)),
        rollups => Ok(Json.toJson(rollups))
      )
    }
  }

  val monthRollupsService = MonthRollupsService.default()

  def monthRollups = Auth.async { implicit request =>
    withRawPaginationParams { rawPaginationParams =>
      monthRollupsService.getMonthRollups(
        contextGuids = request.queryString.get("contextGuid").map { rawGuids =>
          rawGuids.map(UUID.fromString)
        },
        userGuids = Some(Seq(request.accessToken.resourceOwner.guid)),
        rawPaginationParams = rawPaginationParams
      ).map { result =>
        result.fold(
          errors => Conflict(Json.toJson(errors)),
          result => paginatedResult(routes.AggregatesController.monthRollups, result)
        )
      }
    }
  }

}
