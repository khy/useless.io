package models.books

import java.util.UUID
import play.api.libs.json._

case class Edition(
  isbn: String,
  title: String,
  subtitle: Option[String],
  authors: Seq[String],
  pageCount: Int,
  imageUrl: Option[String],
  thumbnailUrl: Option[String]
)

object Edition {

  implicit val format = Json.format[Edition]

}
