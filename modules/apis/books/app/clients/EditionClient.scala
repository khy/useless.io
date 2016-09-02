package clients.books

import java.net.URLEncoder
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.WSClient
import play.api.libs.json.JsObject
import org.joda.time.LocalDate

import models.books.{Edition, Provider}

trait EditionClient {

  def query(query: String)(implicit ec: ExecutionContext): Future[Seq[Edition]]

  def findByIsbn(isbns: Seq[String])(implicit ec: ExecutionContext): Future[Seq[Edition]]

}

object GoogleEditionClient {

  def toEdition(jsons: JsObject): Seq[Edition] = {
    (jsons \ "items").as[Seq[JsObject]].map { json =>
      val optIndustyIds = (json \ "volumeInfo" \ "industryIdentifiers").asOpt[Seq[JsObject]]

      def getIndustryId(key: String): Option[String] = optIndustyIds.flatMap { industryIdentifiers =>
        industryIdentifiers.
          find { json => (json \ "type").as[String] == key }.
          map { json => (json \ "identifier").as[String] }
      }

      val optIsbn = getIndustryId("ISBN_13").orElse(getIndustryId("ISBN_10"))

      optIsbn.map { isbn =>
        Edition(
          isbn = isbn,
          title = (json \ "volumeInfo" \ "title").as[String],
          subtitle = (json \ "volumeInfo" \ "subtitle").asOpt[String],
          authors = (json \ "volumeInfo" \ "authors").asOpt[Seq[String]].getOrElse(Seq.empty),
          pageCount = (json \ "volumeInfo" \ "pageCount").asOpt[Int].getOrElse(100),
          smallImageUrl = (json \ "volumeInfo" \ "imageLinks" \ "smallThumbnail").asOpt[String],
          largeImageUrl = (json \ "volumeInfo" \ "imageLinks" \ "thumbnail").asOpt[String],
          publisher = (json \ "volumeInfo" \ "publisher").asOpt[String],
          publishedAt = (json \ "volumeInfo" \ "publishedDate").asOpt[String].map(LocalDate.parse),
          provider = Provider.Google,
          providerId = (json \ "id").asOpt[String]
        )
      }
    }.flatten
  }

}

class GoogleEditionClient(
  ws: WSClient
) extends EditionClient {

  def query(query: String)(implicit ec: ExecutionContext): Future[Seq[Edition]] = {
    val encodedQuery = URLEncoder.encode(query, "utf-8")
    val url = "https://www.googleapis.com/books/v1/volumes?q=" + encodedQuery
    ws.url(url).get().map { response =>
      response.json.asOpt[JsObject].map {
        GoogleEditionClient.toEdition
      }.getOrElse(Seq.empty)
    }
  }

  def findByIsbn(isbns: Seq[String])(implicit ec: ExecutionContext): Future[Seq[Edition]] = {
    val _query = isbns.map { isbn => "isbn:" + isbn }.mkString("&")
    query(_query)
  }

}
