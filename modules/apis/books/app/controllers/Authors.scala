package controllers.books

import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.play.http.QueryStringUtil._

import services.books.AuthorService
import models.books.Author
import models.books.Author.format
import controllers.books.auth.Auth

object Authors extends Controller {

  def index = Action.async { request =>
    AuthorService.findAuthors(
      names = request.laxQueryString.seq[String]("name")
    ).flatMap { authors =>
      AuthorService.db2api(authors).map { authors =>
        Ok(Json.toJson(authors))
      }
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
      ).flatMap { author =>
        AuthorService.db2api(Seq(author)).map { authors =>
          Created(Json.toJson(authors.head))
        }
      }
    )
  }

}
