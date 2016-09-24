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
import clients.books.EditionClient
import services.books.EditionService

class Editions(
  editionClient: EditionClient,
  editionService: EditionService
) extends Controller {

  def index = Action.async { request =>
    request.richQueryString.get[String]("title").flatMap { titles =>
      titles.headOption
    }.map { title =>
      editionClient.query(title).map { editions =>
        Ok(Json.toJson(editions))
      }
    }.getOrElse {
      Future.successful(Conflict("must specify 'title' query param"))
    }
  }

  def get(isbn: String) = Action.async { request =>
    editionService.getEdition(isbn).map { optEdition =>
      optEdition.map { edition =>
        Ok(Json.toJson(edition))
      }.getOrElse {
        NotFound
      }
    }
  }

}
