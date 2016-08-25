package test.util

import scala.concurrent.{ExecutionContext, Future}
import play.api.ApplicationLoader.Context
import io.useless.client.account.{AccountClient, AccountClientComponents}

import init.books.AbstractApplicationComponents
import models.books.Edition
import clients.books.{EditionClient, ClientComponents}

object TestApplicationComponents {

  val editionClient = new TestEditionClient(Seq.empty)

}

class TestApplicationComponents(
  context: Context,
  val accountClient: AccountClient,
  val editionClient: EditionClient = TestApplicationComponents.editionClient
) extends AbstractApplicationComponents(context)
  with AccountClientComponents
  with ClientComponents

class TestEditionClient(editions: Seq[Edition]) extends EditionClient {

  def query(query: String)(implicit ec: ExecutionContext) = {
    Future.successful(editions)
  }

}
