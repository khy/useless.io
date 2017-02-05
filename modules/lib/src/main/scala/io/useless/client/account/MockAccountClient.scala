package io.useless.client.account

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

import io.useless.account.{ Account, AuthorizedUser, User }

class MockAccountClient(
  accounts: Seq[Account]
) extends AccountClient {

  def getAccount(guid: UUID)(implicit ec: ExecutionContext) = {
    findAccounts(Seq(guid)).map(_.headOption)
  }

  def findAccounts(guids: Seq[UUID])(implicit ec: ExecutionContext) = {
    Future.successful(accounts.filter { account =>
      guids.contains(account.guid)
    })
  }

  def getAccountForEmail(email: String)(implicit ec: ExecutionContext) = {
    val optAccount = accounts.find { account =>
      account match {
        case user: AuthorizedUser => user.email == email
        case _ => false
      }
    }

    Future.successful(optAccount)
  }

  def getAccountForHandle(handle: String)(implicit ec: ExecutionContext) = {
    val optAccount = accounts.find { account =>
      account match {
        case user: User => user.handle == handle
        case _ => false
      }
    }

    Future.successful(optAccount)
  }

}
