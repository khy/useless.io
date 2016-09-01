package test.util

import java.util.UUID
import play.api.test.Helpers._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.accesstoken.AccessToken

import init.books.AbstractApplicationComponents
import services.books.db._

class AppHelper(
  applicationComponents: AbstractApplicationComponents
){

  import applicationComponents._

  import dbConfig.db
  import dbConfig.driver.api._

  def clearNotes() {
    db.run(sqlu"delete from notes")
  }

  def addNote(isbn: String, pageNumber: Int, content: String)(implicit accessToken: AccessToken): UUID = await {
    noteService.addNote(isbn, pageNumber, content, accessToken).map(_.toSuccess.value.guid)
  }

}
