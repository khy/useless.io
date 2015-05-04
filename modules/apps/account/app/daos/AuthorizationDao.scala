package daos.account

import java.util.UUID
import scala.concurrent.Future
import reactivemongo.bson._
import reactivemongo.api.indexes.{ Index, IndexType }
import org.joda.time.DateTime
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.reactivemongo.MongoAccessor
import io.useless.reactivemongo.bson.UuidBson._

import daos.account.mongo.AuthorizationDocument

object AuthorizationDao {

  lazy val instance = new AuthorizationDao

}

class AuthorizationDao {

  private lazy val collection = {
    MongoAccessor("account.mongo.uri").collection("authorizations")
  }


  def ensureIndexes() {
    collection.indexesManager.ensure(new Index(
      key = Seq("authorization_code" -> IndexType.Ascending),
      unique = true
    ))
  }

  def create(authorizationCode: UUID, accessTokenGuid: UUID): Future[Either[String, AuthorizationDocument]] = {
    val document = new AuthorizationDocument(
      guid = UUID.randomUUID,
      authorizationCode = authorizationCode,
      accessTokenGuid = accessTokenGuid,
      createdAt = DateTime.now
    )

    collection.insert(document).map { lastError =>
      if (lastError.ok) {
        Right(document)
      } else {
        throw lastError
      }
    }
  }

  def forAuthorizationCode(authorizationCode: UUID): Future[Option[AuthorizationDocument]] = {
    collection.find(BSONDocument("authorization_code" -> authorizationCode)).one[AuthorizationDocument]
  }

}
