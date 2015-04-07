package object support {

  type AccountFactory = models.account.AccountFactory

}

package models.account {

  import java.util.UUID
  import scala.concurrent.Await
  import scala.concurrent.duration._
  import play.api.libs.concurrent.Execution.Implicits._
  import org.joda.time.DateTime
  import io.useless.accesstoken.{ Scope => UselessScope }

  import mongo.AccessToken._
  import mongo.Account._
  import mongo.Api._
  import mongo.App._
  import mongo.User._

  trait AccountFactory {

    val accountCollection = Account.collection

    def createApi(
      key: String,
      scopes: Seq[UselessScope] = Seq()
    ) = {
      val document = new ApiDocument(key)
      createAccountWithAccessToken(api = Some(document), scopes = scopes)
    }

    def createApp(
      name: String,
      url: String,
      scopes: Seq[UselessScope] = Seq()
    ) = {
      val document = new AppDocument(name, url, url + "/auth")
      createAccountWithAccessToken(app = Some(document), scopes = scopes)
    }

    def createUser(
      email: String,
      handle: String,
      name: Option[String],
      scopes: Seq[UselessScope] = Seq()
    ) = {
      val document = new UserDocument(email, handle, name)
      createAccountWithAccessToken(user = Some(document), scopes = scopes)
    }

    def createAccountWithAccessToken(
      api: Option[ApiDocument] = None,
      app: Option[AppDocument] = None,
      user: Option[UserDocument] = None,
      scopes: Seq[UselessScope] = Seq()
    ) = {
      val document = buildAccountDocument(api, app, user, scopes)
      Await.ready(accountCollection.insert(document), 5.seconds)
      new Account(document)
    }

    def buildAccountDocument(
      api: Option[ApiDocument] = None,
      app: Option[AppDocument] = None,
      user: Option[UserDocument] = None,
      scopes: Seq[UselessScope] = Seq()
    ) = {
      new AccountDocument(
        guid = UUID.randomUUID,
        api = api,
        app = app,
        user = user,
        accessTokens = Seq(new AccessTokenDocument(
          guid = UUID.randomUUID,
          authorizationCode = UUID.randomUUID,
          clientGuid = None,
          scopes = scopes,
          createdAt = DateTime.now,
          authorizedAt = None,
          deletedAt = None
        )),
        createdAt = DateTime.now,
        deletedAt = None
      )
    }

  }

}
