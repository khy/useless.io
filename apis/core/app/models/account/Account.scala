package models.core.account

import java.util.UUID
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.api.indexes.{ Index, IndexType }
import io.useless.util.Validator
import io.useless.accesstoken.{ Scope => UselessScope }
import io.useless.account.{ Account => UselessAccount }
import io.useless.reactivemongo.MongoAccess
import io.useless.reactivemongo.bson.UuidBson._
import io.useless.reactivemongo.bson.DateTimeBson._

import mongo.AccessToken._
import mongo.Account._
import mongo.Api._
import mongo.App._
import mongo.User._

object Account extends MongoAccess {

  override def mongoUri = configuration.getString("core.mongo.uri")

  protected[account] lazy val collection = mongo.collection("accounts")

  def ensureIndexes() {
    collection.indexesManager.ensure(new Index(
      key = Seq("access_tokens.guid" -> IndexType.Ascending),
      unique = true
    ))

    collection.indexesManager.ensure(new Index(
      key = Seq("access_tokens.authorization_code" -> IndexType.Ascending),
      unique = true
    ))

    collection.indexesManager.ensure(new Index(
      key = Seq("user.email" -> IndexType.Ascending),
      unique = true,
      sparse = true
    ))

    collection.indexesManager.ensure(new Index(
      key = Seq("user.handle" -> IndexType.Ascending),
      unique = true,
      sparse = true
    ))

    collection.indexesManager.ensure(new Index(
      key = Seq("api.key" -> IndexType.Ascending),
      unique = true,
      sparse = true
    ))

    collection.indexesManager.ensure(new Index(
      key = Seq("app.name" -> IndexType.Ascending),
      unique = true,
      sparse = true
    ))
  }

  def forGuid(guid: UUID): Future[Option[Account]] = {
    findOne(BSONDocument("_id" -> guid))
  }

  def forAccessToken(guid: UUID): Future[Option[Account]] = {
    findOne(BSONDocument("access_tokens.guid" -> guid))
  }

  def forAuthorizationCode(authorizationCode: UUID): Future[Option[Account]] = {
    findOne(BSONDocument("access_tokens.authorization_code" -> authorizationCode))
  }

  def forApiKey(key: String): Future[Option[Account]] = {
    findOne(BSONDocument("api.key" -> key))
  }

  def forAppName(name: String): Future[Option[Account]] = {
    findOne(BSONDocument("app.name" -> name))
  }

  def forEmail(email: String): Future[Option[Account]] = {
    findOne(BSONDocument("user.email" -> email))
  }

  def forHandle(handle: String): Future[Option[Account]] = {
    findOne(BSONDocument("user.handle" -> handle))
  }

  def createApi(key: String): Future[Either[String, Account]] = {
    forApiKey(key).flatMap { maybeAccount =>
      maybeAccount.map { account =>
        Future.successful(Left(s"API account with key '${key}' already exists."))
      }.getOrElse {
        val document = new ApiDocument(key)
        createAccount(api = Some(document))
      }
    }
  }

  def createApp(name: String, url: String, authRedirectUrl: String): Future[Either[String, Account]] = {
    forAppName(name).flatMap { maybeAccount =>
      maybeAccount.map { account =>
        Future.successful(Left(s"App account with name '${name}' already exists."))
      }.getOrElse {
        val document = new AppDocument(name, url, authRedirectUrl)
        createAccount(app = Some(document))
      }
    }
  }

  def createUser(
    email: String,
    handle: String,
    name: Option[String]
  ): Future[Either[String, Account]] = {
    if (!Validator.isValidEmail(email)) {
      Future.successful(Left(s"'${email}' is not a valid email."))
    } else if (!Validator.isValidHandle(handle)) {
      Future.successful(Left(s"'${handle}' is not a valid handle."))
    } else {
      val query = BSONDocument("$or" -> Seq(
        BSONDocument("user.email" -> email),
        BSONDocument("user.handle" -> handle)
      ))

      findOne(query).flatMap { maybeAccount =>
        maybeAccount.flatMap { account =>
          account.user.map { user =>
            val message = if (user.email == email) {
              s"User account with email ${email} already exists."
            } else {
              s"User account with handle ${handle} already exists."
            }

            Future.successful(Left(message))
          }
        }.getOrElse {
          val document = new UserDocument(email, handle, name)
          createAccount(user = Some(document))
        }
      }
    }
  }

  protected[account] def createAccount(
    api: Option[ApiDocument] = None,
    app: Option[AppDocument] = None,
    user: Option[UserDocument] = None
  ): Future[Either[String, Account]] = {
    if (Seq(api, app, user).filter(_.isDefined).length != 1) {
      Future.successful(Left("An account must have exactly one of api, app or user."))
    } else {
      val creationTime = DateTime.now
      val document = new AccountDocument(
        guid = UUID.randomUUID,
        api = api,
        app = app,
        user = user,
        accessTokens = Seq(new AccessTokenDocument(
          guid = UUID.randomUUID,
          authorizationCode = UUID.randomUUID,
          clientGuid = None,
          scopes = Seq(),
          createdAt = creationTime,
          authorizedAt = None,
          deletedAt = None
        )),
        createdAt = creationTime,
        deletedAt = None
      )

      collection.insert(document).map { lastError =>
        if (lastError.ok) {
          Right(new Account(document))
        } else {
          throw lastError
        }
      }
    }
  }

  protected[account] def findOne(query: BSONDocument): Future[Option[Account]] = {
    val document = collection.find(query).one[AccountDocument]
    document.map { optDocument => optDocument.map { new Account(_) } }
  }

}

class Account(document: AccountDocument) {

  val guid = document.guid

  lazy val api = document.api.map { apiDocument =>
    new Api(this, apiDocument)
  }

  lazy val app = document.app.map { appDocument =>
    new App(this, appDocument)
  }

  lazy val user = document.user.map { userDocument =>
    new User(this, userDocument)
  }

  lazy val accessTokens = document.accessTokens.map { accessTokenDocument =>
    new AccessToken(this, accessTokenDocument)
  }

  def reload() = Account.findOne(BSONDocument("_id" -> guid)).map { optAccount =>
    optAccount.getOrElse {
      throw new FailedAccountReloadException(s"Mongo could not find _id [$guid]")
    }
  }

  def addAccessToken(
    clientGuid: Option[UUID],
    scopes: Seq[UselessScope]
  ): Future[Either[String, AccessToken]] = {
    val document = new AccessTokenDocument(
      guid = UUID.randomUUID,
      authorizationCode = UUID.randomUUID,
      clientGuid = clientGuid,
      scopes = scopes,
      createdAt = DateTime.now,
      authorizedAt = None,
      deletedAt = None
    )

    update(BSONDocument(
      "$push" -> BSONDocument("access_tokens" -> document)
    )).map { lastError =>
      if (lastError.ok) {
        Right(new AccessToken(this, document))
      } else {
        throw lastError
      }
    }
  }

  def authorizeAccessToken(accessTokenGuid: UUID): Future[Either[String, DateTime]] =
    setAccessTokenActionDate("authorized", accessTokenGuid)

  def deleteAccessToken(accessTokenGuid: UUID): Future[Either[String, DateTime]] =
    setAccessTokenActionDate("deleted", accessTokenGuid)

  def toPublic: UselessAccount = {
    val optApi = api.map(_.toPublic)
    val optApp = app.map(_.toPublic)
    val optUser = user.map(_.toPublic)

    Seq(optApi, optApp, optUser).flatMap(_.toSeq).headOption.getOrElse {
      throw new InvalidAccountStateException(guid)
    }
  }

  def toAuthorized: UselessAccount = {
    val optApi = api.map(_.toPublic)
    val optApp = app.map(_.toAuthorized)
    val optUser = user.map(_.toAuthorized)

    Seq(optApi, optApp, optUser).flatMap(_.toSeq).headOption.getOrElse {
      throw new InvalidAccountStateException(guid)
    }
  }

  private def update(update: BSONDocument) = {
    val query = BSONDocument("_id" -> guid)
    Account.collection.update(query, update)
  }

  private def setAccessTokenActionDate(action: String, accessTokenGuid: UUID): Future[Either[String, DateTime]] = {
    val date = DateTime.now
    val query = BSONDocument(
      "_id" -> guid,
      "access_tokens" -> BSONDocument(
        "$elemMatch" -> BSONDocument(
          "guid" -> accessTokenGuid,
          "%s_at".format(action) -> BSONDocument(
            "$exists" -> false
          )
        )
      )
    )

    val update = BSONDocument("$set" ->
      BSONDocument("access_tokens.$.%s_at".format(action) -> date)
    )

    Account.collection.update(query, update).map { result =>
      if (result.ok) {
        if (result.updated == 0) {
          Left("The access token has already been %s".format(action))
        } else {
          Right(date)
        }
      } else {
        throw result.getCause
      }
    }
  }

}
