package io.useless.client.account

import org.scalatest.FunSpec
import org.scalatest.Matchers
import java.util.UUID
import play.api.libs.ws.Response
import play.api.libs.json._

import io.useless.account.User
import io.useless.play.json.account.AccountJson._
import io.useless.play.client.MockBaseClientComponent
import io.useless.test.Await

class PlayAccountClientSpec
  extends FunSpec
  with    Matchers
{

  class MockPlayAccountClient(status: Int, json: JsValue)
    extends PlayAccountClient
    with    MockBaseClientComponent
  {

    override lazy val baseClient = new MockBaseClient(status, json)

  }

  describe ("PlayAccountClient#getAccount") {

    it ("should return an Account if the response is successful and can be parsed") {
      val accountGuid = UUID.fromString("a2285a88-10cf-4089-8300-5f82beff46f9")
      val account = User(
        guid = accountGuid,
        handle = "khy",
        name = None
      )

      val accountClient = new MockPlayAccountClient(200, Json.toJson(account))
      val result = Await(accountClient.getAccount(accountGuid))

      result match {
        case Some(user: User) => {
          user.guid should be (accountGuid)
          user.handle should be ("khy")
        }
        case _ => fail("expected account to be a User")
      }
    }

  }

  describe ("PlayAccountClient#getAccountForEmail") {

    it ("should return an Account if the specified email exists") {
      val account = User(
        guid = UUID.randomUUID,
        handle = "khy",
        name = None
      )

      val accountClient = new MockPlayAccountClient(200, Json.arr(account))
      val result = Await(accountClient.getAccountForEmail("khy@me.com"))

      result match {
        case Some(user: User) => {
          user.handle should be ("khy")
        }
        case _ => fail("expected account to be a User")
      }
    }

  }

  describe ("PlayAccountClient#getAccountForHandle") {

    it ("should return an Account if the specified email exists") {
      val account = User(
        guid = UUID.randomUUID,
        handle = "khy",
        name = None
      )

      val accountClient = new MockPlayAccountClient(200, Json.arr(account))
      val result = Await(accountClient.getAccountForHandle("khy"))

      result match {
        case Some(user: User) => {
          user.handle should be ("khy")
        }
        case _ => fail("expected account to be a User")
      }
    }

  }

}
