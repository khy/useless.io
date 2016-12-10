package models.budget

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.useless.account.User
import io.useless.play.json.DateTimeJson._
import io.useless.play.json.NamedEnumJson

import models.budget.aggregates._

object JsonImplicits {

  val userReads: Reads[User] = (
    (__ \ "guid").read[UUID] ~
    (__ \ "handle").read[String] ~
    (__ \ "name").readNullable[String]
  ) { (guid: UUID, handle: String, name: Option[String]) =>
    User(guid, handle, name)
  }

  val userWrites = new Writes[User] {
    def writes(user: User): JsValue = {
      Json.obj(
        "guid" -> user.guid,
        "handle" -> user.handle,
        "name" -> user.name
      )
    }
  }

  implicit val userFormats = Format(userReads, userWrites)

  implicit val accountTypeFormat = NamedEnumJson.keyFormat(AccountType)
  implicit val contextFormat = Json.format[Context]
  implicit val transactionTypeOwnershipFormat = NamedEnumJson.keyFormat(TransactionTypeOwnership)
  implicit val accountFormat = Json.format[Account]
  implicit val transactionTypeFormat = Json.format[TransactionType]
  implicit val transactionFormat = Json.format[Transaction]
  implicit val plannedTransactionFormat = Json.format[PlannedTransaction]
  implicit val transferFormat = Json.format[Transfer]
  implicit val projectionFormat = Json.format[Projection]
  implicit val monthRollupFormat = Json.format[MonthRollup]
  implicit val accountHistoryIntervalFormat = Json.format[AccountHistoryInterval]
  implicit val accountProjectionIntervalFormat = Json.format[AccountProjectionInterval]
  implicit val transactionTypeRollupFormat = Json.format[TransactionTypeRollup]
  implicit val intervalTypeFormat = NamedEnumJson.keyFormat(IntervalType)

}
