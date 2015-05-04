package daos.account.mongo

import reactivemongo.bson._
import java.util.UUID
import org.joda.time.DateTime
import io.useless.reactivemongo.bson.UuidBson._
import io.useless.reactivemongo.bson.DateTimeBson._

class AuthorizationDocument(
  val guid: UUID,
  val authorizationCode: UUID,
  val accessTokenGuid: UUID,
  val createdAt: DateTime
)

object AuthorizationDocument {

  implicit object AuthorizationDocumentReader extends BSONDocumentReader[AuthorizationDocument] {
    def read(bsonDocument: BSONDocument): AuthorizationDocument = {
      new AuthorizationDocument(
        bsonDocument.getAsTry[UUID]("_id").get,
        bsonDocument.getAsTry[UUID]("authorization_code").get,
        bsonDocument.getAsTry[UUID]("access_token_guid").get,
        bsonDocument.getAsTry[DateTime]("created_at").get
      )
    }
  }

  implicit object AuthorizationDocumentWriter extends BSONDocumentWriter[AuthorizationDocument] {
    def write(authorizationDocument: AuthorizationDocument): BSONDocument = {
      BSONDocument(
        "_id" -> authorizationDocument.guid,
        "authorization_code" -> authorizationDocument.authorizationCode,
        "access_token_guid" -> authorizationDocument.accessTokenGuid,
        "created_at" -> authorizationDocument.createdAt
      )
    }
  }

}
