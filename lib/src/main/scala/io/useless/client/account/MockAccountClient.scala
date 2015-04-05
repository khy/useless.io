package io.useless.client.account

import java.util.UUID
import scala.concurrent.Future

import io.useless.account.{ Account, AuthorizedUser, User }

class MockAccountClient(
  accounts: Seq[Account]
) extends AccountClient {

  def getAccount(guid: UUID) = {
    val optAccount = accounts.find { _.guid == guid }
    Future.successful(optAccount)
  }

  def getAccountForEmail(email: String) = {
    val optAccount = accounts.find { account =>
      account match {
        case user: AuthorizedUser => user.email == email
        case _ => false
      }
    }

    Future.successful(optAccount)
  }

  def getAccountForHandle(handle: String) = {
    val optAccount = accounts.find { account =>
      account match {
        case user: User => user.handle == handle
        case _ => false
      }
    }

    Future.successful(optAccount)
  }

}
