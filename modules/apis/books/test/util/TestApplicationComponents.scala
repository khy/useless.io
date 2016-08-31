package test.util

import scala.concurrent.{ExecutionContext, Future}
import play.api.ApplicationLoader.Context
import io.useless.client.account.{AccountClient, AccountClientComponents}
import io.useless.client.accesstoken.{AccessTokenClient, AccessTokenClientComponents}

import init.books.AbstractApplicationComponents
import models.books.Edition
import clients.books.{EditionClient, ClientComponents}

class TestApplicationComponents(
  context: Context,
  val accountClient: AccountClient,
  val accessTokenClient: AccessTokenClient,
  val editionClient: EditionClient
) extends AbstractApplicationComponents(context)
  with AccountClientComponents
  with AccessTokenClientComponents
  with ClientComponents

class TestEditionClient(editions: Seq[Edition]) extends EditionClient {

  def query(query: String)(implicit ec: ExecutionContext) = {
    Future.successful(editions)
  }

  def findByIsbn(isbns: Seq[String])(implicit ec: ExecutionContext) = {
    val edition = editions.filter { edition =>
      isbns.contains(edition.isbn)
    }

    Future.successful(edition)
  }

}
