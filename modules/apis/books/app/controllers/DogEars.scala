package controllers.books

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.play.http.QueryStringUtil._
import io.useless.pagination._
import io.useless.play.pagination.PaginationController
import io.useless.play.json.validation.ErrorsJson._
import io.useless.play.authentication.Authenticated

import services.books.DogEarService
import models.books.DogEar.format

class DogEars(
  authenticated: Authenticated,
  dogEarService: DogEarService
) extends Controller with PaginationController {

  def index = Action.async { implicit request =>
    withRawPaginationParams { rawPaginationParams =>
      dogEarService.findDogEars(
        guids = request.richQueryString.get[UUID]("guid"),
        bookTitles = request.richQueryString.get[String]("bookTitle"),
        accountGuids = request.richQueryString.get[UUID]("accountGuid"),
        rawPaginationParams
      ).flatMap { result =>
        result.fold(
          error => Future.successful(Conflict(Json.toJson(error))),
          result => dogEarService.db2api(result.items).map { dogEars =>
            paginatedResult(routes.DogEars.index(), result.copy(items = dogEars))
          }
        )
      }
    }
  }

  case class NewDogEar(isbn: String, pageNumber: Int, note: Option[String])
  private implicit val newDogEarReads = Json.reads[NewDogEar]

  def create = authenticated.async(parse.json) { request =>
    request.body.validate[NewDogEar].fold(
      error => Future.successful(Conflict),
      newDogEar => {
        dogEarService.addDogEar(
          isbn = newDogEar.isbn,
          pageNumber = newDogEar.pageNumber,
          note = newDogEar.note,
          accessToken = request.accessToken
        ).flatMap { result =>
          result.fold(
            error => Future.successful(Conflict(Json.toJson(error))),
            dogEar => dogEarService.db2api(Seq(dogEar)).map { dogEars =>
              Created(Json.toJson(dogEars.head))
            }
          )
        }
      }
    )
  }

}
