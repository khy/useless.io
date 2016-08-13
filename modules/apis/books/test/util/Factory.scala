package test.util

import java.util.UUID
import play.api.test.Helpers._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.accesstoken.AccessToken

import services.books._

object Factory {

  val noteService = NoteService.instance()

  def addNote(isbn: String, pageNumber: Int, content: String)(implicit accessToken: AccessToken): UUID = await {
    noteService.addNote(isbn, pageNumber, content, accessToken).map(_.toSuccess.value.guid)
  }

}
