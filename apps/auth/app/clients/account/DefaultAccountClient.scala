package clients.auth.account

import java.util.UUID
import scala.concurrent.Future
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import io.useless.account.{ Account, App, AuthorizedApp, User }
import io.useless.play.client.ResourceClientComponent
import io.useless.play.json.account.AccountJson._

import clients.auth.UnexpectedAccountTypeException

trait DefaultAccountClientComponent {

  self: ResourceClientComponent =>

  class DefaultAccountClient(
    resourceClient: ResourceClient
  ) extends AccountClient {

    def getApp(guid: UUID) = {
      resourceClient.get[Account](s"/accounts/$guid").map { optAccount =>
        optAccount.map { account =>
          account match {
            case authorizedApp: AuthorizedApp => authorizedApp
            case account => throw new UnexpectedAccountTypeException(account, "AuthorizedApp")
          }
        }
      }
    }

    def getUserForEmail(email: String) = {
      resourceClient.find[Account]("/accounts", "email" -> email).map { result =>
        result.items.headOption.map { account =>
          account match {
            case user: User => user
            case account => throw new UnexpectedAccountTypeException(account, "User")
          }
        }
      }
    }

    def createUser(email: String, handle: Option[String], name: Option[String]) = {
      resourceClient.create[Account]("/users", Json.obj(
        "email" -> email,
        "handle" -> handle,
        "name" -> name
      )).map { result =>
        result.right.map { account =>
          account match {
            case user: User => user
            case account => throw new UnexpectedAccountTypeException(account, "User")
          }
        }
      }
    }

  }

}
