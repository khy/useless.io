package io.useless.play.json.account

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._

import io.useless.account.{ User, AuthorizedUser }
import io.useless.play.json.UuidJson._

object AuthorizedUserJson {

  val reads: Reads[AuthorizedUser] = (
    (__ \ "guid").read[UUID] ~
    (__ \ "user" \ "email").read[String] ~
    (__ \ "user" \ "handle").read[String] ~
    (__ \ "user" \ "name").read[Option[String]]
  ) { (guid: UUID, email: String, handle: String, name: Option[String]) =>
    User.authorized(guid, email, handle, name)
  }

  val writes = new Writes[AuthorizedUser] {
    def writes(user: AuthorizedUser): JsValue = {
      Json.obj(
        "guid" -> user.guid,
        "user" -> Json.obj(
          "email" -> user.email,
          "handle" -> user.handle,
          "name" -> user.name
        )
      )
    }
  }

}
