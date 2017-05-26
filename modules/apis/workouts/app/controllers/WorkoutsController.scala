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
import services.workouts.WorkoutsService

class WorkoutsController(
  authenticated: Authenticated,
  workoutsService: WorkoutsService
) extends Controller with PaginationController {

  // TODO: move to lib somewhere
  private def toErrors(playError: (JsPath, Seq[ValidationError])): Errors = playError match {
    case (jsPath, errors) => Errors(
      key = Some(jsPath.toString),
      messages = errors.map { error => Message(error.message) }
    )
  }

  def create = authenticated.async(parse.json) { request =>
    request.body.validate[core.Workout].fold(
      error => Future.successful(BadRequest(error.toString)),
      workout => workoutsService.addWorkout(
        workout = workout,
        accessToken = request.accessToken
      ).flatMap { result =>
        result.fold(
          errors => Future.successful(BadRequest(Json.toJson(errors.map(toErrors)))),
          workout => workoutsService.db2api(Seq(workout)).map { workouts =>
            Created(Json.toJson(workouts.head))
          }
        )
      }
    )
  }

}
