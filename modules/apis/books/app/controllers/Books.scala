package controllers.books

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.play.http.QueryStringUtil._

import services.books.BookService
import models.books.Book.format
import controllers.books.auth.Auth

object Books extends Controller {

  lazy val bookService = BookService.instance()

  def index = Action.async { request =>
    bookService.findBooks(
      guids = request.richQueryString.get[UUID]("guid"),
      titles = request.richQueryString.get[String]("title")
    ).flatMap { books =>
      bookService.db2api(books).map { books =>
        Ok(Json.toJson(books))
      }
    }
  }

  case class NewBook(title: String, authorGuid: UUID)
  private implicit val newBookReads = Json.reads[NewBook]

  def create = Auth.async(parse.json) { request =>
    request.body.validate[NewBook].fold(
      error => Future.successful(Conflict),
      newBook => bookService.addBook(
        title = newBook.title,
        authorGuid = newBook.authorGuid,
        accessToken = request.accessToken
      ).flatMap { book =>
        bookService.db2api(Seq(book)).map { books =>
          Created(Json.toJson(books.head))
        }
      }
    )
  }

}
