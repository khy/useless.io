package test.util

import java.util.UUID
import play.api.test.Helpers._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.accesstoken.AccessToken

import services.books._

object Factory {

  val authorService = AuthorService.instance()
  val bookService = BookService.instance()
  val editionService = EditionService.instance()
  val noteService = NoteService.instance()

  def addAuthor(name: String)(implicit accessToken: AccessToken): UUID = await {
    authorService.addAuthor(name, accessToken).map(_.guid)
  }

  def addBook(title: String, authorName: String)(implicit accessToken: AccessToken): UUID = {
    val authorGuid = addAuthor(authorName)
    addBook(title, authorGuid)
  }

  def addBook(title: String, authorGuid: UUID)(implicit accessToken: AccessToken): UUID = await {
    bookService.addBook(title, authorGuid, accessToken).map(_.guid)
  }

  def addEdition(bookGuid: UUID, pageCount: Int)(implicit accessToken: AccessToken): UUID = await {
    editionService.addEdition(bookGuid, pageCount, accessToken).map(_.toSuccess.value.guid)
  }

  def addNote(editionGuid: UUID, pageNumber: Int, content: String)(implicit accessToken: AccessToken): UUID = await {
    noteService.addNote(editionGuid, pageNumber, content, accessToken).map(_.toSuccess.value.guid)
  }

}
