package test.util

import java.util.UUID
import play.api.test.Helpers._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.accesstoken.AccessToken

import services.books._

object Factory {

  def addAuthor(name: String)(implicit accessToken: AccessToken): UUID = await {
    AuthorService.addAuthor(name, accessToken).map(_.guid)
  }

  def addBook(title: String, authorName: String)(implicit accessToken: AccessToken): UUID = {
    val authorGuid = addAuthor(authorName)
    addBook(title, authorGuid)
  }

  def addBook(title: String, authorGuid: UUID)(implicit accessToken: AccessToken): UUID = await {
    BookService.addBook(title, authorGuid, accessToken).map(_.guid)
  }

  def addEdition(bookGuid: UUID, pageCount: Int)(implicit accessToken: AccessToken): UUID = await {
    EditionService.addEdition(bookGuid, pageCount, accessToken).map(_.right.get.guid)
  }

  def addNote(editionGuid: UUID, pageNumber: Int, content: String)(implicit accessToken: AccessToken): UUID = await {
    NoteService.addNote(editionGuid, pageNumber, content, accessToken).map(_.right.get.guid)
  }

}
