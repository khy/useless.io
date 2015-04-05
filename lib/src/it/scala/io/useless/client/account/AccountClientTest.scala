package io.useless.client.account

import java.util.UUID
import org.scalatest.FunSuite
import org.scalatest.Matchers
import scala.concurrent.Await
import scala.concurrent.duration._

import io.useless.account.User

class AccountClientTest
  extends FunSuite
  with    Matchers
{

  val client = AccountClient.instance

  test ("An Account can be retrieved") {
    val guid = UUID.fromString("572cedcc-5b3a-4b45-b789-09d3d89bdc1b")
    val account = Await.result(client.getAccount(guid), 1.second)
    account.get.guid should be (guid)
  }

  test ("An Account can be retrieved by email") {
    val account = Await.result(client.getAccountForEmail("khy@me.com"), 1.second)
    account.get match {
      case user: User => user.handle should be (Some("khy"))
      case _ => fail("expected account to be a User")
    }
  }

  test ("An Account can be retrieved by handle") {
    val account = Await.result(client.getAccountForHandle("khy"), 1.second)
    account.get match {
      case user: User => user.handle should be (Some("khy"))
      case _ => fail("expected account to be a User")
    }
  }

}
