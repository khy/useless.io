package io.useless.client.account

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.{FunSpec, Matchers}

import io.useless.account.User
import io.useless.test.Await

class MockAccountClientSpec
  extends FunSpec
  with    Matchers
{

  val khy = User.authorized(
    guid = UUID.randomUUID,
    email = "khy@me.com",
    handle = "khy",
    name = None
  )

  val bob = User.authorized(
    guid = UUID.randomUUID,
    email = "bob@useless.io",
    handle = "bob",
    name = None
  )

  val client = new MockAccountClient(Seq(khy, bob))

  describe ("MockAccountClient#getAccount") {

    it ("should return the Account corresponding to the specified GUID") {
      val user = Await(client.getAccount(khy.guid)).get.asInstanceOf[User]
      user.handle should be ("khy")
    }

    it ("should return None if the specified GUID doesn't exist") {
      Await(client.getAccount(UUID.randomUUID)) should be (None)
    }

  }

  describe ("MockAccountClient#getAccountForEmail") {

    it ("should return the User corresponding to the specified email") {
      val user = Await(client.getAccountForEmail("khy@me.com")).get.asInstanceOf[User]
      user.handle should be ("khy")
    }

    it ("should return None if the specified email doesn't exist") {
      Await(client.getAccountForEmail("no-one@useless.io")) should be (None)
    }

  }

  describe ("MockAccountClient#getAccountForHandle") {

    it ("should return the User corresponding to the specified handle") {
      val user = Await(client.getAccountForHandle("khy")).get.asInstanceOf[User]
      user.handle should be ("khy")
    }

    it ("should return None if the specified handle doesn't exist") {
      Await(client.getAccountForHandle("no-one")) should be (None)
    }

  }

}
