package clients.books

import play.api.BuiltInComponents
import play.api.libs.ws.ning.NingWSComponents

import io.useless.client.account.AccountClient
import io.useless.util.configuration.RichConfiguration._

trait ClientComponents {
  def editionClient: EditionClient
}

trait ProdClientComponents extends ClientComponents {

  self: BuiltInComponents with NingWSComponents =>

  val accessTokenGuid = configuration.underlying.getUuid("books.accessTokenGuid")

  lazy val editionClient: EditionClient = new GoogleEditionClient(wsClient)

}
