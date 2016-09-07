package test.util

import scala.concurrent.{ExecutionContext, Future}

import models.books.{Edition, Provider}
import clients.books.EditionClient

object MockEditionClient {

  val default = new MockEditionClient(Seq(
    MockEdition.theMarriagePlot1,
    MockEdition.theMarriagePlot2,
    MockEdition.iPassLikeNight1
  ))

}

class MockEditionClient(editions: Seq[Edition]) extends EditionClient {

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

object MockEdition {

  val theMarriagePlot1 = Edition(
    isbn = "1111111111111",
    title = "The Marriage Plot",
    subtitle = Some("A Novel"),
    authors = Seq("Jeffrey Eugenides"),
    pageCount = 406,
    smallImageUrl = Some("example.com/marriageplot/sm"),
    largeImageUrl = Some("example.com/marriageplot/lg"),
    publisher = None,
    publishedAt = None,
    provider = Provider.Google,
    providerId = None
  )

  val theMarriagePlot2 = Edition(
    isbn = "2222222222222",
    title = "The Marriage Plot",
    subtitle = Some("A Novel"),
    authors = Seq("Jeffrey Eugenides"),
    pageCount = 426,
    smallImageUrl = Some("example.com/marriageplot/sm"),
    largeImageUrl = Some("example.com/marriageplot/lg"),
    publisher = None,
    publishedAt = None,
    provider = Provider.Google,
    providerId = None
  )

  val iPassLikeNight1 = Edition(
    isbn = "3333333333333",
    title = "I Pass Like Night",
    subtitle = None,
    authors = Seq("Jonathan Ames"),
    pageCount = 164,
    smallImageUrl = Some("example.com/passlikenight/sm"),
    largeImageUrl = Some("example.com/passlikenight/lg"),
    publisher = None,
    publishedAt = None,
    provider = Provider.Google,
    providerId = None
  )

}
