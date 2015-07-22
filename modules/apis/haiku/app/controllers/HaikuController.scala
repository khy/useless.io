package controllers.haiku

import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._
import io.useless.play.authentication.Authenticated
import io.useless.account.User

import services.haiku.HaikuService
import models.haiku.json.HaikuJson._
import lib.haiku.Pagination
import controllers.haiku.auth.Auth

object HaikuController extends Controller {

  def index = Action.async { request =>
    val userHandle = request.getQueryString("user")
    val pagination = Pagination(request)

    HaikuService.find(userHandle, pagination).map { haikus =>
      Ok(Json.toJson(haikus))
    }
  }

  def create = Auth.async(parse.json) { request =>
    val lines = (request.body \ "lines").as[Seq[String]]

    request.accessToken.resourceOwner match {
      case user: User => HaikuService.create(user, lines).map { result =>
        result.fold (
          errors => UnprocessableEntity(Json.toJson(errors)),
          haiku => Created(Json.toJson(haiku))
        )
      }
      case _ => Future.successful(Unauthorized)
    }
  }

}
