package io.useless.client.account

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.FunSpec
import org.scalatest.Matchers
import play.api.libs.ws.{WS, WSResponse}
import play.api.libs.json._
import org.scalatestplus.play.OneAppPerSuite

import io.useless.account.User
import io.useless.play.json.account.AccountJson._
import io.useless.play.client._
import io.useless.test.{ Await, ImplicitPlayApplication }

class PlayAccountClientSpec
  extends FunSpec
  with    Matchers
  with    ImplicitPlayApplication
{

  class MockPlayAccountClient(status: Int, json: JsValue)
    extends PlayAccountClient(WS.client, "", UUID.randomUUID)
  {

    override lazy val resourceClient = {
      val baseClient = new MockBaseClient(status, json)
      val jsonClient = new DefaultJsonClient(baseClient)
      new DefaultResourceClient(jsonClient)
    }

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
