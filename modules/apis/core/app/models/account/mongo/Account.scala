package models.core.account.mongo

import java.util.UUID
import reactivemongo.bson._
import org.joda.time.DateTime
import io.useless.reactivemongo.bson.UuidBson._
import io.useless.reactivemongo.bson.DateTimeBson._

import AccessToken._
import Api._
import App._
import User._

object Account {

  class AccountDocument(
    val guid: UUID,
    val api: Option[ApiDocument],
    val app: Option[AppDocument],
    val user: Option[UserDocument],
    val accessTokens: Seq[AccessTokenDocument],
    val createdAt: DateTime,
    val deletedAt: Option[DateTime]
  )

  implicit object AccountBSONReader extends BSONDocumentReader[AccountDocument] {
    def read(account: BSONDocument): AccountDocument = {
      new AccountDocument(
        account.getAsTry[UUID]("_id").get,
        account.getAs[ApiDocument]("api"),
        account.getAs[AppDocument]("app"),
        account.getAs[UserDocument]("user"),
        account.getAsTry[Seq[AccessTokenDocument]]("access_tokens").get,
        account.getAsTry[DateTime]("created_at").get,
        account.getAs[DateTime]("deleted_at")
      )
    }
  }

  implicit object AccountBSONWriter extends BSONDocumentWriter[AccountDocument] {
    def write(account: AccountDocument): BSONDocument = {
      BSONDocument(
        "_id" -> account.guid,
        "api" -> account.api,
        "app" -> account.app,
        "user" -> account.user,
        "access_tokens" -> account.accessTokens,
        "created_at" -> account.createdAt,
        "deleted_at" -> account.deletedAt
      )
    }
  }

}
