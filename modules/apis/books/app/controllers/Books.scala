package controllers.books

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.play.pagination.PaginationController
import io.useless.play.http.QueryStringUtil._
import io.useless.play.json.validation.ErrorsJson._

import services.books.BookService
import models.books.Book._

class Books(
  bookService: BookService
) extends Controller with PaginationController {

  def index = Action.async { implicit request =>
    withRawPaginationParams { rawPaginationParams =>
      bookService.findBooks(
        titles = request.richQueryString.get[String]("title"),
        rawPaginationParams
      ).map { result =>
        result.fold(
          error => Conflict(Json.toJson(error)),
          result => paginatedResult(routes.Books.index(), result)
        )
      }
    }
  }

}
