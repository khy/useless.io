package controllers.books

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.play.authentication.Authenticated
import io.useless.util.Uuid
import io.useless.pagination._
import io.useless.play.pagination.PaginationController
import io.useless.play.json.ClientErrorJson
import io.useless.http.LinkHeader

import services.books.NoteService
import models.books.Note.format

object Notes extends Controller with PaginationController {

  import ClientErrorJson.format

  def get(guid: UUID) = Authenticated.async {
    NoteService.getNote(guid).map { optNote =>
      optNote.map { note =>
        Ok(Json.toJson(note))
      }.getOrElse(NotFound)
    }
  }

  def index = Authenticated.async { implicit request =>
    val accountGuids = request.queryString.get("account_guid").map { rawGuids =>
      rawGuids.map(Uuid.parseUuid(_)).filter(_.isSuccess).map(_.get)
    }.getOrElse(Seq.empty)

    withRawPaginationParams { rawPaginationParams =>
      NoteService.findNotes(
        accountGuids,
        rawPaginationParams
      ).map { result =>
        result.fold(
          error => Conflict(Json.toJson(error)),
          result => paginatedResult(routes.Notes.index(), result)
        )
      }
    }
  }

  case class NewNote(edition_guid: UUID, page_number: Int, content: String)
  private implicit val newNoteReads = Json.reads[NewNote]

  def create = Authenticated.async(parse.json) { request =>
    request.body.validate[NewNote].fold(
      error => Future.successful(Conflict),
      newNote => {
        NoteService.addNote(
          editionGuid = newNote.edition_guid,
          pageNumber = newNote.page_number,
          content = newNote.content,
          accessToken = request.accessToken
        ).map { result =>
          result.fold(
            error => Conflict(Json.toJson(error)),
            note => Created(Json.toJson(note))
          )
        }
      }
    )
  }

}