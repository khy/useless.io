package models.core.account

import org.scalatest._
import org.scalatestplus.play._
import play.api.test.Helpers
import Helpers._
import io.useless.util.mongo.MongoUtil

import mongo.Api._
import mongo.App._
import support.MongoHelper

class AccountSpec
  extends PlaySpec
  with    BeforeAndAfterEach
  with    AccountFactory
{

  override def beforeEach {
    MongoHelper.clearDb()
  }

  "Account.createAccount" should {
    "fail if neither an api, app or user is specified" in {
      val result = Helpers.await { Account.createAccount() }
      result must be ('left)
    }

    "fail if more than one of api, app or user is specified" in {
      val api = new ApiDocument("museum")
      val app = new AppDocument("Hinterland", "hinterland.io", "hinterland.io/auth")
      val result = Helpers.await { Account.createAccount(api = Some(api), app = Some(app)) }
      result must be ('left)
    }

    "create a default, 1st-party access token for the new account" in {
      val api = new ApiDocument("museum")
      val result = Helpers.await { Account.createAccount(api = Some(api)) }

      result must be ('right)
      val account = result.right.get
      account.accessTokens.length mustBe 1
      val accessToken = account.accessTokens(0)
      accessToken.clientGuid mustBe (None)
    }
  }

  "Account#guid" should {
    "delegate to the underlying document" in {
      val document = buildAccountDocument(app = Some(new AppDocument("App", "app.com", "app.com/auth")))
      val account = new Account(document)
      account.guid mustBe(document.guid)
    }
  }

  "Account#authorizeAccessToken" should {
    "set authorizedAt for the access token" in {
      val user = createUser("khy@useless.io", "khy", None)
      val accessToken = user.accessTokens(0)
      val result = Helpers.await { user.authorizeAccessToken(accessToken.guid) }
      result must be ('right)

      val _accessToken = Helpers.await { user.reload() }.accessTokens(0)
      _accessToken must be ('authorized)
    }

    "not set authorizedAt if it has already been authorized" in {
      val user = createUser("khy@useless.io", "khy", None)
      val accessToken = user.accessTokens(0)
      val firstResult = Helpers.await { user.authorizeAccessToken(accessToken.guid) }
      val originalAuthorizedDate = firstResult.right.get

      Thread.sleep(1000)
      val secondResult = Helpers.await { user.authorizeAccessToken(accessToken.guid) }
      secondResult must be ('left)

      val _accessToken = Helpers.await { user.reload() }.accessTokens(0)
      _accessToken.authorizedAt mustBe (Some(originalAuthorizedDate))
    }
  }

  "Account#deleteAccessToken" should {
    "set the deletedAt for the access token" in {
      val user = createUser("khy@useless.io", "khy", None)
      val accessToken = user.accessTokens(0)
      val result = Helpers.await { user.deleteAccessToken(accessToken.guid) }
      result must be ('right)

      val _accessToken = Helpers.await { user.reload() }.accessTokens(0)
      _accessToken must be ('deleted)
    }

    "not set deletedAt if it has already been deleted" in {
      val user = createUser("khy@useless.io", "khy", None)
      val accessToken = user.accessTokens(0)
      val firstResult = Helpers.await { user.deleteAccessToken(accessToken.guid) }
      val originalDeletedDate = firstResult.right.get

      Thread.sleep(1000)
      val secondResult = Helpers.await { user.deleteAccessToken(accessToken.guid) }
      secondResult must be ('left)

      val _accessToken = Helpers.await { user.reload() }.accessTokens(0)
      _accessToken.deletedAt mustBe (Some(originalDeletedDate))
    }

    "only delete the specified access token" in {
      val user = createUser("khy@useless.io", "khy", None)
      val app = createApp("Account", "account.useless.io", Seq.empty)
      val accessToken = Helpers.await { user.addAccessToken(Some(app.guid), Seq.empty) }.right.get
      Helpers.await { user.deleteAccessToken(accessToken.guid) }
      val _user = Helpers.await { user.reload() }
      _user.accessTokens.headOption must be ('defined)
    }

    "appropriately set deletedAt if there are multiple, deleted access tokens" in {
      val user = createUser("khy@useless.io", "khy", None)
      val app = createApp("Account", "account.useless.io", Seq.empty)
      val accessToken = Helpers.await { user.addAccessToken(Some(app.guid), Seq.empty) }.right.get
      Helpers.await { user.deleteAccessToken(user.accessTokens(0).guid) }
      Helpers.await { user.deleteAccessToken(accessToken.guid) }
      val _user = Helpers.await { user.reload() }
      _user.accessTokens.find(_.guid == accessToken.guid).head must be ('deleted)
    }
  }

}
