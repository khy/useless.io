package controllers.budget

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.play.json.DateTimeJson._
import io.useless.play.json.MessageJson.format
import io.useless.play.pagination.PaginationController

import controllers.budget.auth.Auth
import services.budget.ContextsService
import models.budget.JsonImplicits._

object ContextsController extends Controller with PaginationController {

  val contextsService = ContextsService.default()

  def index = Auth.async { implicit request =>
    withRawPaginationParams { rawPaginationParams =>
      contextsService.findContexts(
        userGuids = Some(Seq(request.accessToken.resourceOwner.guid)),
        rawPaginationParams = rawPaginationParams
      ).flatMap { result =>
        result.fold(
          errors => Future.successful(Conflict(Json.toJson(errors))),
          result => contextsService.records2models(result.items).map { contexts =>
            val _result = result.copy(items = contexts)
            paginatedResult(routes.ContextsController.index, _result)
          }
        )
      }
    }
  }

  case class CreateData(
    name: String,
    userGuids: Seq[UUID]
  )
  private implicit val cdr = Json.reads[CreateData]

  def create = Auth.async(parse.json) { request =>
    request.body.validate[CreateData].fold(
      error => Future.successful(Conflict(error.toString)),
      data => contextsService.createContext(
        name = data.name,
        userGuids = data.userGuids,
        accessToken = request.accessToken
      ).flatMap { result =>
        result.fold(
          errors => Future.successful(Conflict(Json.toJson(errors))),
          contextRecord => {
            contextsService.records2models(Seq(contextRecord)).map { contexts =>
              Created(Json.toJson(contexts.head))
            }
          }
        )
      }
    )
  }

}
