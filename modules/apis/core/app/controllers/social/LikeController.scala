package controllers.core.social

import java.util.UUID
import scala.concurrent.Future
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._
import io.useless.play.json.MessageJson.format
import io.useless.play.pagination.PaginationController

import controllers.core.auth.Auth
import models.core.social.JsonImplicits._
import services.core.social.LikeService

object LikeController extends Controller with PaginationController {

  val likeService = LikeService.instance(current.configuration)

  case class IndexQuery(
    resourceApi: Option[String],
    resourceType: Option[String],
    resourceId: Option[String]
  )

  val indexQueryForm = Form(
    mapping(
      "resourceApi" -> optional(text),
      "resourceType" -> optional(text),
      "resourceId" -> optional(text)
    )(IndexQuery.apply)(IndexQuery.unapply)
  )

  def index = Auth.async { implicit request =>
    indexQueryForm.bindFromRequest.fold(
      formWithErrors => Future.successful(Conflict(formWithErrors.errorsAsJson)),
      indexQuery => withRawPaginationParams { pagination =>
        likeService.find(
          indexQuery.resourceApi,
          indexQuery.resourceType,
          indexQuery.resourceId,
          pagination
        ).flatMap { result =>
          result.fold(
            errors => Future.successful(Conflict(Json.toJson(errors))),
            result => likeService.db2model(result.items).map { likeModels =>
              paginatedResult(routes.LikeController.index(), result.copy(items = likeModels))
            }
          )
        }
      }
    )
  }

  def create(
    resourceApi: String,
    resourceType: String,
    resourceId: String
  ) = Auth.async(parse.json) { request =>
    likeService.create(resourceApi, resourceType, resourceId, request.accessToken).flatMap { result =>
      result.fold(
        failure => Future.successful(Conflict(Json.toJson(failure))),
        like => likeService.db2model(Seq(like)).map { likes =>
          Created(Json.toJson(likes.head))
        }
      )
    }
  }

  def delete(
    resourceApi: String,
    resourceType: String,
    resourceId: String
  ) = Auth.async { request =>
    likeService.delete(resourceApi, resourceType, resourceId, request.accessToken).flatMap { optLike =>
      optLike.map { like =>
        likeService.db2model(Seq(like)).map { likes =>
          Ok(Json.toJson(likes.head))
        }
      }.getOrElse {
        Future.successful(NotFound)
      }
    }
  }

}
