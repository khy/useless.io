package test.util

import java.util.UUID
import play.api.BuiltInComponents
import play.api.test.Helpers._
import play.api.db.slick.SlickComponents
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.client.account.AccountClientComponents
import io.useless.accesstoken.AccessToken

import init.books.AbstractApplicationComponents
import clients.books.ClientComponents
import services.books.ServiceComponents
import services.books.db.DbConfigComponents
import services.books.db._

class AppHelper(
  applicationComponents: AbstractApplicationComponents with ClientComponents with AccountClientComponents
)
  extends SlickComponents
  with AccountClientComponents
  with DbConfigComponents
  with ClientComponents
  with ServiceComponents
{

  import dbConfig.db
  import dbConfig.driver.api._

  def applicationLifecycle = applicationComponents.applicationLifecycle
  def configuration = applicationComponents.configuration
  def environment = applicationComponents.environment

  def accountClient = applicationComponents.accountClient
  def editionClient = applicationComponents.editionClient

  def clearDb() {
    val query = Notes.filter { r => r.guid === r.guid }
    db.run(query.result)
  }

  def addNote(isbn: String, pageNumber: Int, content: String)(implicit accessToken: AccessToken): UUID = await {
    noteService.addNote(isbn, pageNumber, content, accessToken).map(_.toSuccess.value.guid)
  }

}
