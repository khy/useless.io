package clients.books

import java.net.URLEncoder
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.WSClient
import play.api.libs.json.JsObject

import models.books.ExternalBook

trait ExternalBookClient {

  def query(query: String)(implicit ec: ExecutionContext): Future[Seq[ExternalBook]]

}

object GoogleBooksClient {

  def toExternalBook(jsons: JsObject): Seq[ExternalBook] = {
    println(jsons)
    (jsons \ "items").as[Seq[JsObject]].map { json =>
      ExternalBook(
        title = (json \ "volumeInfo" \ "title").as[String],
        author = (json \ "volumeInfo" \ "authors").asOpt[Seq[String]].map(_.head),
        editions = Seq.empty
      )
    }
  }

}

class GoogleBooksClient(
  ws: WSClient
) extends ExternalBookClient {

  def query(query: String)(implicit ec: ExecutionContext): Future[Seq[ExternalBook]] = {
    val encodedQuery = URLEncoder.encode(query, "utf-8")
    val url = "https://www.googleapis.com/books/v1/volumes?q=" + encodedQuery
    ws.url(url).get().map { response =>
      response.json.asOpt[JsObject].map {
        GoogleBooksClient.toExternalBook
      }.getOrElse(Seq.empty)
    }
  }

}
