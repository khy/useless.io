package models.haiku

import java.util.UUID
import play.api.libs.json._
import io.useless.account.{PublicUser, User}
import io.useless.play.json.DateTimeJson._

object JsonImplicits {

  implicit val userWrites = Writes { user: User =>
    Json.obj(
      "guid" -> user.guid,
      "handle" -> user.handle,
      "name" -> user.name
    )
  }

  implicit val userReads = Reads[User] { json: JsValue =>
    val user = new PublicUser(
      (json \ "guid").as[UUID],
      (json \ "handle").as[String],
      (json \ "name").asOpt[String]
    )

    new JsSuccess(user)
  }

  implicit val shallowHaikuFormat = Json.format[ShallowHaiku]
  implicit val haikuFormat = Json.format[Haiku]

}
