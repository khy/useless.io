package models.books

import java.util.UUID
import play.api.libs.json._

case class Book(
  title: String,
  subtitle: Option[String],
  authors: Seq[String],
  smallImageUrl: Option[String],
  largeImageUrl: Option[String]
)

object Book {
  implicit val formatBook = Json.format[Book]
}
