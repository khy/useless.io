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
import io.useless.play.json.DateTimeJson._
import io.useless.play.json.MessageJson.format
import io.useless.play.pagination.PaginationController

import controllers.budget.auth.Auth
import services.budget.ProjectionsService
import models.budget.JsonImplicits._

object ProjectionsController extends Controller {

  val projectionsService = ProjectionsService.default()

  def index = Auth.async { implicit request =>
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

}
