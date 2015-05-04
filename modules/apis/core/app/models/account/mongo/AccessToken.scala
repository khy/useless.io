package models.core.account.mongo

import java.util.UUID
import reactivemongo.bson._
import org.joda.time.DateTime
import io.useless.accesstoken.{ Scope => UselessScope }
import io.useless.reactivemongo.bson.UuidBson._
import io.useless.reactivemongo.bson.DateTimeBson._

object AccessToken {

  class AccessTokenDocument(
    val guid: UUID,
    val authorizationCode: UUID,
    val clientGuid: Option[UUID],
    val scopes: Seq[UselessScope],
    val createdAt: DateTime,
    val authorizedAt: Option[DateTime],
    val deletedAt: Option[DateTime]
  )

  implicit object AccessTokenBSONReader extends BSONDocumentReader[AccessTokenDocument] {
    def read(accessToken: BSONDocument): AccessTokenDocument = {
      new AccessTokenDocument(
        accessToken.getAsTry[UUID]("guid").get,
        accessToken.getAsTry[UUID]("authorization_code").get,
        accessToken.getAs[UUID]("client_guid"),
        accessToken.getAs[Seq[String]]("scopes").getOrElse(Seq()).map(UselessScope(_)),
        accessToken.getAsTry[DateTime]("created_at").get,
        accessToken.getAs[DateTime]("authorized_at"),
        accessToken.getAs[DateTime]("deleted_at")
      )
    }
  }

  implicit object AccessTokenBSONWriter extends BSONDocumentWriter[AccessTokenDocument] {
    def write(accessToken: AccessTokenDocument): BSONDocument = {
      BSONDocument(
        "guid" -> accessToken.guid,
        "authorization_code" -> accessToken.authorizationCode,
        "client_guid" -> accessToken.clientGuid,
        "scopes" -> accessToken.scopes.map(_.toString),
        "created_at" -> accessToken.createdAt,
        "authorized_at" -> accessToken.authorizedAt,
        "deleted_at" -> accessToken.deletedAt
      )
    }
  }

}
