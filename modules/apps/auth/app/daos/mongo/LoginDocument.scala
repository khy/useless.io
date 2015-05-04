package daos.auth.mongo

import reactivemongo.bson._
import java.util.UUID
import org.joda.time.DateTime
import io.useless.reactivemongo.bson.UuidBson._
import io.useless.reactivemongo.bson.DateTimeBson._

class LoginDocument(
  val guid: UUID,
  val userGuid: UUID,
  val accessTokenGuid: UUID,
  val hashedPassword: String,
  val createdAt: DateTime,
  val deletedAt: Option[DateTime]
)

object LoginDocument {

  implicit object LoginDocumentReader extends BSONDocumentReader[LoginDocument] {
    def read(bsonDocument: BSONDocument): LoginDocument = {
      new LoginDocument(
        bsonDocument.getAs[UUID]("_id").get,
        bsonDocument.getAs[UUID]("user_guid").get,
        bsonDocument.getAs[UUID]("access_token_guid").get,
        bsonDocument.getAs[String]("hashed_password").get,
        bsonDocument.getAs[DateTime]("created_at").get,
        bsonDocument.getAs[DateTime]("deleted_at")
      )
    }
  }

  implicit object LoginDocumentWriter extends BSONDocumentWriter[LoginDocument] {
    def write(loginDocument: LoginDocument): BSONDocument = {
      BSONDocument(
        "_id" -> loginDocument.guid,
        "user_guid" -> loginDocument.userGuid,
        "access_token_guid" -> loginDocument.accessTokenGuid,
        "hashed_password" -> loginDocument.hashedPassword,
        "created_at" -> loginDocument.createdAt,
        "deleted_at" -> loginDocument.deletedAt
      )
    }
  }

}
