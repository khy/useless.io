package controllers.books

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.play.json.MessageJson

import services.books.EditionService
import models.books.Edition.format
import controllers.books.auth.Auth

object Editions extends Controller {

  import MessageJson.format

  case class NewEdition(bookGuid: UUID, pageCount: Int)
  private implicit val newEditionReads = Json.reads[NewEdition]

  def create = Auth.async(parse.json) { request =>
    request.body.validate[NewEdition].fold(
      error => Future.successful(Conflict),
      newEdition => {
        EditionService.addEdition(
          bookGuid = newEdition.bookGuid,
          pageCount = newEdition.pageCount,
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
