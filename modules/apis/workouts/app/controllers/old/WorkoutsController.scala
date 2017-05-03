package controllers.workouts.old

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.play.json.validation.ErrorsJson._
import io.useless.play.http.QueryStringUtil._
import io.useless.play.pagination.PaginationController
import io.useless.play.authentication.Authenticated

import models.workouts.old._
import models.workouts.old.JsonImplicits._
import services.workouts.old.WorkoutsService

class WorkoutsController(
  authenticated: Authenticated,
  workoutsService: WorkoutsService
) extends Controller with PaginationController {

  def index = Action.async { implicit request =>
    withRawPaginationParams { rawPaginationParams =>
      workoutsService.findWorkouts(
        guids = request.richQueryString.get[UUID]("guid"),
        parentGuids = request.richQueryString.get[UUID]("parentGuid"),
        isChild = request.richQueryString.get[Boolean]("child").flatMap(_.headOption),
        rawPaginationParams
      ).flatMap { result =>
        result.fold(
          error => Future.successful(Conflict(Json.toJson(error))),
          result => workoutsService.db2api(result.items).map { workouts =>
            paginatedResult(routes.WorkoutsController.index(), result.copy(items = workouts))
          }
        )
      }
    }
  }

  def createChild(parentGuid: UUID) = create(Some(parentGuid))

  def create(parentGuid: Option[UUID] = None) = authenticated.async(parse.json) { request =>
    request.body.validate[core.Workout].fold(
      error => Future.successful(BadRequest(error.toString)),
      workout => workoutsService.addWorkout(
        parentGuid = parentGuid,
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
