package controllers.books

import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.play.http.QueryStringUtil._

import services.books.AuthorService
import models.books.Author.format
import controllers.books.auth.Auth

object Authors extends Controller {

  lazy val authorService = AuthorService.instance()

  def index = Action.async { request =>
    authorService.findAuthors(
      names = request.richQueryString.get[String]("name")
    ).flatMap { authors =>
      authorService.db2api(authors).map { authors =>
        Ok(Json.toJson(authors))
      }
    }
  }

  case class NewAuthor(name: String)
  private implicit val newAuthorReads = Json.reads[NewAuthor]

  def create = Auth.async(parse.json) { request =>
    request.body.validate[NewAuthor].fold(
      error => Future.successful(Conflict),
      newAuthor => authorService.addAuthor(
        name = newAuthor.name,
        accessToken = request.accessToken
      ).flatMap { author =>
        authorService.db2api(Seq(author)).map { authors =>
          Created(Json.toJson(authors.head))
        }
      }
    )
  }

}
