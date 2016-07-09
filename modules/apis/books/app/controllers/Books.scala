package controllers.books

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.play.http.QueryStringUtil._

import services.books.BookService
import models.books.Book
import models.books.Book.format
import controllers.books.auth.Auth

object Books extends Controller {

  def get(guid: UUID) = Action.async {
    BookService.getBook(guid).map { optBook =>
      optBook.map { book =>
        Ok(Json.toJson(book))
      }.getOrElse(NotFound)
    }
  }

  def index = Action.async { request =>
    BookService.findBooks(
      titles = request.laxQueryString.seq[String]("title")
    ).map { books =>
      Ok(Json.toJson(books))
    }
  }

  case class NewBook(title: String, authorGuid: UUID)
  private implicit val newBookReads = Json.reads[NewBook]

  def create = Auth.async(parse.json) { request =>
    request.body.validate[NewBook].fold(
      error => Future.successful(Conflict),
      newBook => BookService.addBook(
        title = newBook.title,
        authorGuid = newBook.authorGuid,
        accessToken = request.accessToken
      ).map { book =>
        Created(Json.toJson(book))
      }
    )
  }

}
