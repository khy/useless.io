package services.budget.util

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}
import play.api.{Play, Application}
import play.api.libs.ws.WS
import io.useless.account.{User, PublicUser}
import io.useless.client.account.AccountClient
import io.useless.util.configuration.RichConfiguration._

object UsersHelper {

  def default()(implicit app: Application) = {
    new UsersHelper(AccountClient.instance(
      client = WS.client,
      baseUrl = Play.configuration.underlying.getString("useless.core.baseUrl"),
      authGuid = Play.configuration.underlying.getUuid("budget.accessTokenGuid")
    ))
  }

  val AnonUser: User = new PublicUser(
    guid = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    handle = "anon",
    name = None
  )

}

class UsersHelper(
  accountClient: AccountClient
) {

  def getUsers(guids: Seq[UUID])(implicit ec: ExecutionContext): Future[Seq[User]] = {
    val userOptFuts = guids.map { guid =>
      accountClient.getAccount(guid).map { optAccount =>
        optAccount match {
          case Some(user: User) => Some(user)
          case _ => None
        }
      }
    }

    Future.sequence(userOptFuts).map { userOpts =>
      userOpts.filter(_.isDefined).map(_.get)
    }
  }

}
