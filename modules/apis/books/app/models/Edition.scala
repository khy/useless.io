package models.books

import java.util.UUID
import play.api.libs.json._

case class Edition(
  guid: UUID,
  pageCount: Int
)

object Edition {

  implicit val format = Json.format[Edition]

}
