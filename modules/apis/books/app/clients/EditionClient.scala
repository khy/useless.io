package clients.books

import java.net.URLEncoder
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.WSClient
import play.api.libs.json.JsObject

import models.books.Edition

trait EditionClient {

  def query(query: String)(implicit ec: ExecutionContext): Future[Seq[Edition]]

}

object GoogleEditionClient {

  def toEdition(jsons: JsObject): Seq[Edition] = {
    (jsons \ "items").as[Seq[JsObject]].map { json =>

      Edition(
        isbn = "DUMMY-ISBN",
        title = (json \ "volumeInfo" \ "title").as[String],
        subtitle = (json \ "volumeInfo" \ "subtitle").asOpt[String],
        authors = (json \ "volumeInfo" \ "authors").asOpt[Seq[String]].getOrElse(Seq.empty),
        pageCount = (json \ "volumeInfo" \ "pageCount").asOpt[Int].getOrElse(100),
        imageUrl = (json \ "volumeInfo" \ "imageLinks" \ "thumbnail").asOpt[String],
        thumbnailUrl = (json \ "volumeInfo" \ "imageLinks" \ "smallThumbnail").asOpt[String]
      )
    }
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

}
