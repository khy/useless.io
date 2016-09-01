package test.util

import scala.concurrent.{ExecutionContext, Future}

import models.books.Edition
import clients.books.EditionClient

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
