package controllers.core.social

import java.util.UUID
import scala.concurrent.Future
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._
import io.useless.play.json.validation.ErrorsJson._
import io.useless.play.pagination.PaginationController
import io.useless.play.http.LooseQueryStringUtil.RichQueryStringRequest

import controllers.core.auth.Auth
import models.core.social.JsonImplicits._
import services.core.social.LikeService

object LikeController extends Controller with PaginationController {

  val likeService = LikeService.instance(current.configuration)

  def index = Action.async { implicit request =>
    withRawPaginationParams { pagination =>
      likeService.find(
        request.richQueryString.seqString("resourceApi"),
        request.richQueryString.seqString("resourceType"),
        request.richQueryString.seqString("resourceId"),
        request.richQueryString.seqUuid("accountGuid"),
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
  }

  def aggregates = Action.async { implicit request =>
    withRawPaginationParams { pagination =>
      likeService.aggregates(
        request.richQueryString.seqString("resourceApi"),
        request.richQueryString.seqString("resourceType"),
        request.richQueryString.seqString("resourceId"),
        request.richQueryString.seqUuid("accountGuid"),
        pagination
      ).map { result =>
        result.fold(
          errors => Conflict(Json.toJson(errors)),
          result => paginatedResult(routes.LikeController.aggregates(), result)
        )
      }
    }
  }

  def create(
    resourceApi: String,
    resourceType: String,
    resourceId: String
  ) = Auth.async { request =>
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
