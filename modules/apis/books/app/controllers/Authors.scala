package controllers.books

import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import services.books.AuthorService
import models.books.Author
import models.books.Author.format
import controllers.books.auth.Auth

object Authors extends Controller {

  def index(name: String) = Action.async {
    AuthorService.findAuthors(name).map { authors =>
      Ok(Json.toJson(authors))
    }
  }

  case class NewAuthor(name: String)
  private implicit val newAuthorReads = Json.reads[NewAuthor]

  def create = Auth.async(parse.json) { request =>
    request.body.validate[NewAuthor].fold(
      error => Future.successful(Conflict),
      newAuthor => AuthorService.addAuthor(
        name = newAuthor.name,
        accessToken = request.accessToken
      ).map { author =>
        Created(Json.toJson(author))
      }
    )
  }

}
