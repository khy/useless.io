package models.core.account

import org.specs2.mutable.{ Specification, Before }
import play.api.test.Helpers
import Helpers._
import io.useless.util.mongo.MongoUtil

import mongo.Api._
import mongo.App._
import support.MongoHelper

class AccountSpec
  extends Specification
  with    AccountFactory
  with    MongoHelper
{

  trait Context extends Before {
    def before = clearDb()
  }

  "Account.createAccount" should {
    "fail if neither an api, app or user is specified" in {
      val result = Helpers.await { Account.createAccount() }
      result must beLeft
    }

    "fail if more than one of api, app or user is specified" in {
      val api = new ApiDocument("museum")
      val app = new AppDocument("Hinterland", "hinterland.io", "hinterland.io/auth")
      val result = Helpers.await { Account.createAccount(api = Some(api), app = Some(app)) }
      result must beLeft
    }

    "create a default, 1st-party access token for the new account" in {
      val api = new ApiDocument("museum")
      val result = Helpers.await { Account.createAccount(api = Some(api)) }

      result must beRight
      val account = result.right.get
      account.accessTokens.length mustEqual 1
      val accessToken = account.accessTokens(0)
      accessToken.clientGuid must beNone
    }
  }

  "Account#guid" should {
    "delegate to the underlying document" in {
      val document = buildAccountDocument(app = Some(new AppDocument("App", "app.com", "app.com/auth")))
      val account = new Account(document)
      account.guid must beEqualTo(document.guid)
    }
  }

  "Account#authorizeAccessToken" should {
    "set authorizedAt for the access token" in new Context {
      val user = createUser("khy@useless.io", "khy", None)
      val accessToken = user.accessTokens(0)
      val result = Helpers.await { user.authorizeAccessToken(accessToken.guid) }
      result should beRight

      val _accessToken = Helpers.await { user.reload() }.accessTokens(0)
      _accessToken.isAuthorized should beTrue
    }

    "not set authorizedAt if it has already been authorized" in new Context {
      val user = createUser("khy@useless.io", "khy", None)
      val accessToken = user.accessTokens(0)
      val firstResult = Helpers.await { user.authorizeAccessToken(accessToken.guid) }
      val originalAuthorizedDate = firstResult.right.get

      Thread.sleep(1000)
      val secondResult = Helpers.await { user.authorizeAccessToken(accessToken.guid) }
      secondResult should beLeft

      val _accessToken = Helpers.await { user.reload() }.accessTokens(0)
      _accessToken.authorizedAt should beEqualTo (Some(originalAuthorizedDate))
    }
  }

  "Account#deleteAccessToken" should {
    "set the deletedAt for the access token" in new Context {
      val user = createUser("khy@useless.io", "khy", None)
      val accessToken = user.accessTokens(0)
      val result = Helpers.await { user.deleteAccessToken(accessToken.guid) }
      result should beRight

      val _accessToken = Helpers.await { user.reload() }.accessTokens(0)
      _accessToken.isDeleted should beTrue
    }

    "not set deletedAt if it has already been deleted" in new Context {
      val user = createUser("khy@useless.io", "khy", None)
      val accessToken = user.accessTokens(0)
      val firstResult = Helpers.await { user.deleteAccessToken(accessToken.guid) }
      val originalDeletedDate = firstResult.right.get

      Thread.sleep(1000)
      val secondResult = Helpers.await { user.deleteAccessToken(accessToken.guid) }
      secondResult should beLeft

      val _accessToken = Helpers.await { user.reload() }.accessTokens(0)
      _accessToken.deletedAt should beEqualTo (Some(originalDeletedDate))
    }

    "only delete the specified access token" in new Context {
      val user = createUser("khy@useless.io", "khy", None)
      val app = createApp("Account", "account.useless.io", Seq.empty)
      val accessToken = Helpers.await { user.addAccessToken(Some(app.guid), Seq.empty) }.right.get
      Helpers.await { user.deleteAccessToken(accessToken.guid) }
      val _user = Helpers.await { user.reload() }
      _user.accessTokens(0).isDeleted should beFalse
    }

    "appropriately set deletedAt if there are multiple, deleted access tokens" in new Context {
      val user = createUser("khy@useless.io", "khy", None)
      val app = createApp("Account", "account.useless.io", Seq.empty)
      val accessToken = Helpers.await { user.addAccessToken(Some(app.guid), Seq.empty) }.right.get
      Helpers.await { user.deleteAccessToken(user.accessTokens(0).guid) }
      Helpers.await { user.deleteAccessToken(accessToken.guid) }
      val _user = Helpers.await { user.reload() }
      _user.accessTokens.find(_.guid == accessToken.guid).head.isDeleted should beTrue
    }
  }

}
