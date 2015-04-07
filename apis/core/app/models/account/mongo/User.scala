package models.core.account.mongo

import reactivemongo.bson._
import org.joda.time.DateTime

object User {

  class UserDocument(
    val email: String,
    val handle: String,
    val name: Option[String]
  )

  implicit object UserBsonReader extends BSONDocumentReader[UserDocument] {
    def read(user: BSONDocument): UserDocument = {
      new UserDocument(
        user.getAsTry[String]("email").get,
        user.getAsTry[String]("handle").get,
        user.getAs[String]("name")
      )
    }
  }

  implicit object UserBsonWriter extends BSONDocumentWriter[UserDocument] {
    def write(user: UserDocument): BSONDocument = {
      BSONDocument(
        "email" -> user.email,
        "handle" -> user.handle,
        "name" -> user.name
      )
    }
  }

}
