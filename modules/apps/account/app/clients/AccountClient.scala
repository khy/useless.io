package clients.account

import java.util.UUID
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import io.useless.client.ClientException
import io.useless.play.client.ResourceClient
import io.useless.accesstoken.AccessToken
import io.useless.account.{ Account, AuthorizedUser }
import io.useless.play.json.account.AccountJson._
import io.useless.util.configuration.Configuration

object AccountClient
  extends DefaultAccountClientComponent
  with    Configuration
{

  def instance(accessToken: AccessToken): AccountClient = {
    val baseUrl = configuration.underlying.getString("useless.core.baseUrl")
    val resourceClient = ResourceClient(baseUrl, accessToken.guid.toString)
    new DefaultAccountClient(resourceClient)
  }

}

trait AccountClientComponent {

  trait AccountClient {

    def getUser(guid: UUID): Future[Option[AuthorizedUser]]

  }

}

trait DefaultAccountClientComponent extends AccountClientComponent {

  class UnexpectedAccountTypeException(account: Account, expectedType: String)
    extends ClientException(s"Expected account [$account.guid] to be a $expectedType")

  class DefaultAccountClient(
    resourceClient: ResourceClient
  ) extends AccountClient {

    def getUser(guid: UUID) = {
      resourceClient.get[Account](s"/accounts/$guid").map { optAccount =>
        optAccount.map { account =>
          account match {
            case authorizedUser: AuthorizedUser => authorizedUser
            case account => throw new UnexpectedAccountTypeException(account, "AuthorizedUser")
          }
        }
      }
    }

  }

}
