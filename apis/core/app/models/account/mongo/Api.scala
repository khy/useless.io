package models.core.account.mongo

import reactivemongo.bson._
import org.joda.time.DateTime

object Api {

  class ApiDocument(
    val key: String
  )

  implicit object ApiBsonReader extends BSONDocumentReader[ApiDocument] {
    def read(api: BSONDocument): ApiDocument = {
      new ApiDocument(
        api.getAsTry[String]("key").get
      )
    }
  }

  implicit object ApiBsonWriter extends BSONDocumentWriter[ApiDocument] {
    def write(api: ApiDocument): BSONDocument = {
      BSONDocument(
        "key" -> api.key
      )
    }
  }

}
