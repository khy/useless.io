package controllers.books

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.play.json.ClientErrorJson

import services.books.EditionService
import models.books.Edition.format
import controllers.books.auth.Auth

object Editions extends Controller {

  import ClientErrorJson.format

  case class NewEdition(book_guid: UUID, page_count: Int)
  private implicit val newEditionReads = Json.reads[NewEdition]

  def create = Auth.async(parse.json) { request =>
    request.body.validate[NewEdition].fold(
      error => Future.successful(Conflict),
      newEdition => {
        EditionService.addEdition(
          bookGuid = newEdition.book_guid,
          pageCount = newEdition.page_count,
          accessToken = request.accessToken
        ).map { result =>
          result.fold(
            error => Conflict(Json.toJson(error)),
            edition => Created(Json.toJson(edition))
          )
        }
      }
    )
  }

}
