package controllers.workouts

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.play.json.validation.ErrorsJson._
import io.useless.play.pagination.PaginationController
import io.useless.play.authentication.Authenticated

import models.workouts._
import models.workouts.JsonImplicits._
import services.workouts.WorkoutsService

class WorkoutsController(
  authenticated: Authenticated,
  workoutsService: WorkoutsService
) extends Controller with PaginationController {

  def index = Action.async { implicit request =>
    Future.successful(Ok)
  }

  def create = authenticated.async(parse.json) { request =>
    request.body.validate[core.Workout].fold(
      error => Future.successful(BadRequest(error.toString)),
      workout => workoutsService.addWorkout(
        workout = workout,
        accessToken = request.accessToken
      ).flatMap { result =>
        result.fold(
          error => Future.successful(BadRequest(Json.toJson(error))),
          workout => workoutsService.db2api(Seq(workout)).map { workouts =>
            Created(Json.toJson(workouts.head))
          }
        )
      }
    )
  }

}
