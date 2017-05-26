package controllers.workouts

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.{Json, JsPath}
import play.api.data.validation.ValidationError
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.Message
import io.useless.validation.Errors
import io.useless.play.json.validation.ErrorsJson._
import io.useless.play.http.QueryStringUtil._
import io.useless.play.pagination.PaginationController
import io.useless.play.authentication.Authenticated

import models.workouts._
import services.workouts.MovementsService

class MovementsController(
  authenticated: Authenticated,
  movementsService: MovementsService
) extends Controller with PaginationController {

  def index = Action.async { implicit request =>
    withRawPaginationParams { rawPaginationParams =>
      movementsService.findMovements(
        names = request.richQueryString.get[String]("name"),
        rawPaginationParams
      ).map { result =>
        result.fold(
          error => Conflict(Json.toJson(error)),
          result => paginatedResult(routes.MovementsController.index(), result)
        )
      }
    }
  }

  // TODO: move to lib somewhere
  private def toErrors(playError: (JsPath, Seq[ValidationError])): Errors = playError match {
    case (jsPath, errors) => Errors(
      key = Some(jsPath.toString),
      messages = errors.map { error => Message(error.message) }
    )
  }

  def create = authenticated.async(parse.json) { request =>
    request.body.validate[core.Movement].fold(
      error => Future.successful(BadRequest(error.toString)),
      movement => movementsService.addMovement(
        movement = movement,
        accessToken = request.accessToken
      ).flatMap { result =>
        result.fold(
          errors => Future.successful(BadRequest(Json.toJson(errors.map(toErrors)))),
          movement => movementsService.db2api(Seq(movement)).map { movements =>
            Created(Json.toJson(movements.head))
          }
        )
      }
    )
  }

}
