package models.core.account.mongo

import reactivemongo.bson._
import org.joda.time.DateTime

object App {

  class AppDocument(
    val name: String,
    val url: String,
    val authRedirectUrl: String
  )

  implicit object AppBsonReader extends BSONDocumentReader[AppDocument] {
    def read(app: BSONDocument): AppDocument = {
      new AppDocument(
        app.getAsTry[String]("name").get,
        app.getAsTry[String]("url").get,
        app.getAsTry[String]("auth_redirect_url").get
      )
    }
  }

  implicit object AppBsonWriter extends BSONDocumentWriter[AppDocument] {
    def write(app: AppDocument): BSONDocument = {
      BSONDocument(
        "name" -> app.name,
        "url" -> app.url,
        "auth_redirect_url" -> app.authRedirectUrl
      )
    }
  }

}
