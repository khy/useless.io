package models.books

import java.util.UUID
import play.api.libs.json._

case class Book(
  guid: UUID,
  title: String,
  author: Author,
  editions: Seq[Edition]
)

case class ExternalBook(
  title: String,
  author: Option[String],
  editions: Seq[Edition]
)

object Book {
  implicit val formatBook = Json.format[Book]
  implicit val formatExternalBook = Json.format[ExternalBook]
}
