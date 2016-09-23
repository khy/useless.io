package clients.books

import java.net.URLEncoder
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.WSClient
import play.api.libs.json.JsObject
import org.joda.time.LocalDate

import models.books.{Edition, Provider}

trait EditionClient {

  def query(query: String)(implicit ec: ExecutionContext): Future[Seq[Edition]]

  def getByIsbn(isbn: String)(implicit ec: ExecutionContext): Future[Option[Edition]]

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

      val optId = (json \ "id").asOpt[String]

      def generateImageUrl(zoom: Int)  = optId.map { id =>
        s"http://books.google.com/books/content?id=${id}&printsec=frontcover&img=1&zoom=${zoom}"
      }

      optIsbn.map { isbn =>
        Edition(
          isbn = isbn,
          title = (json \ "volumeInfo" \ "title").as[String],
          subtitle = (json \ "volumeInfo" \ "subtitle").asOpt[String],
          authors = (json \ "volumeInfo" \ "authors").asOpt[Seq[String]].getOrElse(Seq.empty),
          pageCount = (json \ "volumeInfo" \ "pageCount").asOpt[Int].getOrElse(100),
          smallImageUrl = generateImageUrl(2),
          largeImageUrl = generateImageUrl(4),
          publisher = (json \ "volumeInfo" \ "publisher").asOpt[String],
          publishedAt = (json \ "volumeInfo" \ "publishedDate").asOpt[String].map(LocalDate.parse),
          provider = Provider.Google,
          providerId = optId
        )
      }
    }.flatten
  }

}

class GoogleEditionClient(
  ws: WSClient,
  apiKey: String
) extends EditionClient {

  def query(query: String)(implicit ec: ExecutionContext): Future[Seq[Edition]] = {
    val encodedQuery = URLEncoder.encode(query, "utf-8")
    val url = s"https://www.googleapis.com/books/v1/volumes?q=${encodedQuery}&key=${apiKey}"
    ws.url(url).get().map { response =>
      response.json.asOpt[JsObject].map {
        GoogleEditionClient.toEdition
      }.getOrElse(Seq.empty)
    }
  }

  def getByIsbn(isbn: String)(implicit ec: ExecutionContext): Future[Option[Edition]] = {
    query("isbn:" + isbn).map(_.headOption)
  }

}
