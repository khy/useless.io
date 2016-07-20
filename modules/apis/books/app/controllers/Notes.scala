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

import services.books.NoteService
import models.books.Note.format
import controllers.books.auth.Auth

object Notes extends Controller with PaginationController {

  val noteService = NoteService.instance()

  def index = Action.async { implicit request =>
    withRawPaginationParams { rawPaginationParams =>
      noteService.findNotes(
        guids = request.richQueryString.get[UUID]("guid"),
        accountGuids = request.richQueryString.get[UUID]("accountGuid"),
        rawPaginationParams
      ).flatMap { result =>
        result.fold(
          error => Future.successful(Conflict(Json.toJson(error))),
          result => noteService.db2api(result.items).map { notes =>
            paginatedResult(routes.Notes.index(), result.copy(items = notes))
          }
        )
      }
    }
  }

  case class NewNote(editionGuid: UUID, pageNumber: Int, content: String)
  private implicit val newNoteReads = Json.reads[NewNote]

  def create = Auth.async(parse.json) { request =>
    request.body.validate[NewNote].fold(
      error => Future.successful(Conflict),
      newNote => {
        noteService.addNote(
          editionGuid = newNote.editionGuid,
          pageNumber = newNote.pageNumber,
          content = newNote.content,
          accessToken = request.accessToken
        ).flatMap { result =>
          result.fold(
            error => Future.successful(Conflict(Json.toJson(error))),
            note => noteService.db2api(Seq(note)).map { notes =>
              Created(Json.toJson(notes.head))
            }
          )
        }
      }
    )
  }

}
