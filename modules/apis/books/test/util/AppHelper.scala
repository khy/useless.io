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

  def clearDogEars() {
    db.run(sqlu"delete from dog_ears")
  }

  def clearEditionCache() {
    db.run(sqlu"delete from edition_cache")
  }

  def addDogEar(isbn: String, pageNumber: Int, note: Option[String])(implicit accessToken: AccessToken): UUID = await {
    dogEarService.addDogEar(isbn, pageNumber, note, accessToken).map(_.toSuccess.value.guid)
  }

}
