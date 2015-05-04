package models.books

import java.util.UUID
import play.api.libs.json._

case class Book(
  guid: UUID,
  title: String,
  author: Author,
  editions: Seq[Edition]
)

object Book {

  implicit val format = Json.format[Book]

}
