package controllers.core.social

import java.util.UUID
import scala.concurrent.Future
import play.api.mvc._
import play.api.Play.current
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._
import io.useless.play.json.MessageJson.format

import controllers.core.auth.Auth
import models.core.social.JsonImplicits._
import services.core.social.LikeService

object LikeController extends Controller {

  val likeService = LikeService.instance(current.configuration)

  def create(
    resourceApi: String,
    resourceType: String,
    resourceId: String
  ) = Auth.async(parse.json)  { request =>
    likeService.create(resourceApi, resourceType, resourceId, request.accessToken).flatMap { result =>
      result.fold(
        failure => Future.successful(Conflict(Json.toJson(failure))),
        like => likeService.db2model(Seq(like)).map { likes =>
          Created(Json.toJson(likes.head))
        }
      )
    }
  }

}
