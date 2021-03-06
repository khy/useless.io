package controllers.haiku

import java.util.UUID
import scala.concurrent.Future
import scala.util.control.Exception._
import play.api._
import play.api.Play.current
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._
import io.useless.play.json.validation.ErrorsJson._
import io.useless.play.pagination.PaginationController
import io.useless.account.User
import io.useless.play.http.QueryStringUtil._

import services.haiku.HaikuService
import models.haiku.JsonImplicits._
import controllers.haiku.auth.Auth

object HaikuController extends Controller with PaginationController {

  def index = Action.async { implicit request =>
    withRawPaginationParams { pagination =>
      HaikuService.find(
        guids = request.richQueryString.get[UUID]("guid"),
        userHandles = request.richQueryString.get[String]("user"),
        inResponseToGuids = request.richQueryString.get[UUID]("inResponseTo"),
        rawPaginationParams = pagination
      ).flatMap { result =>
        result.fold(
          error => Future.successful(Conflict(Json.toJson(error))),
          result => HaikuService.db2model(result.items).map { haikuModels =>
            paginatedResult(routes.HaikuController.index(), result.copy(items = haikuModels))
          }
        )
      }
    }
  }

  case class CreateData(
    lines: Seq[String],
    inResponseToGuid: Option[UUID],
    attribution: Option[String]
  )
  private implicit val cdr = Json.reads[CreateData]

  def create = Auth.async(parse.json) { request =>
    request.body.validate[CreateData].fold(
      error => Future.successful(Conflict(error.toString)),
      data => HaikuService.create(data.inResponseToGuid, data.lines, data.attribution, request.accessToken).flatMap { result =>
        result.fold (
          failure => Future.successful(Conflict(Json.toJson(failure))),
          haiku => HaikuService.db2model(Seq(haiku)).map { haikuModels =>
            Created(Json.toJson(haikuModels.head))
          }
        )
      }
    )
  }

}
