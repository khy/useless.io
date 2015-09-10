package controllers.budget

import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.play.json.MessageJson.format
import io.useless.play.pagination.PaginationController

import controllers.budget.auth.Auth
import services.budget.ProjectionsService
import models.budget.JsonImplicits._

object ProjectionsController extends Controller with PaginationController {

  val projectionsService = ProjectionsService.default()

  def index = Auth.async { implicit request =>
    withRawPaginationParams { rawPaginationParams =>
      projectionsService.findProjections(
        createdByAccounts = Some(Seq(request.accessToken.resourceOwner.guid)),
        rawPaginationParams = rawPaginationParams
      ).map { result =>
        result.fold(
          errors => Conflict(Json.toJson(errors)),
          projections => paginatedResult(routes.ProjectionsController.index, projections)
        )
      }
    }
  }

  case class CreateData(
    name: String
  )
  private implicit val cdr = Json.reads[CreateData]

  def create = Auth.async(parse.json) { request =>
    request.body.validate[CreateData].fold(
      error => Future.successful(Conflict(error.toString)),
      data => projectionsService.createProjection(
        name = data.name,
        accessToken = request.accessToken
      ).map { result =>
        result.fold(
          errors => Conflict(Json.toJson(errors)),
          projection => Created(Json.toJson(projection))
        )
      }
    )
  }

}
