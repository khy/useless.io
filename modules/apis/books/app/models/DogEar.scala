package models.books

import java.util.UUID
import java.sql.Timestamp
import play.api.libs.json._
import org.joda.time.DateTime
import io.useless.account.Account
import io.useless.play.json.account.AccountJson
import io.useless.play.json.DateTimeJson._

case class DogEar(
  guid: UUID,
  edition: Edition,
  pageNumber: Int,
  note: Option[String],
  createdBy: Account,
  createdAt: DateTime
)

object DogEar {

  implicit private val accountFormat =
    Format(AccountJson.accountReads, AccountJson.accountWrites)

  implicit val format = Json.format[DogEar]

}
