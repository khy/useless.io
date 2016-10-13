package models.books

import java.util.UUID
import play.api.libs.json._
import org.joda.time.DateTime

case class UserEdition(
  lastDogEaredAt: DateTime,
  edition: Edition
)

object UserEdition {

  implicit val format = Json.format[UserEdition]

}
