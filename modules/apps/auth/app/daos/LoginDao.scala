package daos.auth

import java.util.UUID
import scala.concurrent.Future
import reactivemongo.bson._
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.reactivemongo.MongoAccessor
import io.useless.reactivemongo.bson.UuidBson._

import mongo.LoginDocument
import LoginDocument._

object LoginDao {

  lazy val instance = new LoginDao

}

class LoginDao {

  private lazy val collection = {
    MongoAccessor("auth.mongo.uri").collection("haikus")
  }

  def getLoginForUserGuid(userGuid: UUID): Future[Option[LoginDocument]] = {
    collection.find(BSONDocument("user_guid" -> userGuid)).one[LoginDocument]
  }

  def createLogin(
    userGuid: UUID,
    accessTokenGuid: UUID,
    password: String
  ): Future[Either[String, LoginDocument]] = {
    val document = new LoginDocument(
      guid = UUID.randomUUID,
      userGuid = userGuid,
      accessTokenGuid = accessTokenGuid,
      hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt),
      createdAt = DateTime.now,
      deletedAt = None
    )

    collection.insert(document).map { lastError =>
      if (lastError.ok) {
        Right(document)
      } else {
        throw lastError
      }
    }
  }

}
