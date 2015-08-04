package controllers.haiku

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._
import io.useless.play.json.MessageJson.format
import io.useless.play.pagination.PaginationController
import io.useless.account.User

import services.haiku.HaikuService
import models.haiku.JsonImplicits._
import controllers.haiku.auth.Auth

object HaikuController extends Controller with PaginationController {

  def index = Action.async { implicit request =>
    withRawPaginationParams { pagination =>
      val userHandle = request.getQueryString("user")

      HaikuService.find(userHandle, pagination).map { result =>
        result.fold(
          error => Conflict(Json.toJson(error)),
          haikus => paginatedResult(routes.HaikuController.index(), haikus)
        )
      }
    }
  }

  def create = Auth.async(parse.json) { request =>
    val inResponseToGuid = (request.body \ "inResponseToGuid").as[Option[UUID]]
    val lines = (request.body \ "lines").as[Seq[String]]

    request.accessToken.resourceOwner match {
      case user: User => HaikuService.create(inResponseToGuid, lines, user).map { result =>
        result.fold (
          failure => UnprocessableEntity(Json.toJson(failure)),
          haiku => Created(Json.toJson(haiku))
        )
      }
      case _ => Future.successful(Unauthorized)
    }
  }

}
