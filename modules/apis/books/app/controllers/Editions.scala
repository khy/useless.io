package controllers.books

import java.util.UUID
import scala.concurrent.Future
import play.api.Play.current
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.play.http.QueryStringUtil._
import io.useless.play.json.validation.ErrorsJson._

import models.books.Edition.format
import controllers.books.auth.Auth
import clients.books._

class Editions(editionClient: EditionClient) extends Controller {

  def index = Action.async { request =>
    request.richQueryString.get[String]("title").flatMap { titles =>
      titles.headOption
    }.map { title =>
      editionClient.query(title).map { externalBooks =>
        Ok(Json.toJson(externalBooks))
      }
    }.getOrElse {
      Future.successful(Conflict("must specify 'title' query param"))
    }
  }

}
