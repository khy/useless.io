package io.useless.play.json.account

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._

import io.useless.account.{ User, PublicUser }
import io.useless.play.json.UuidJson._

object PublicUserJson {

  val reads: Reads[PublicUser] = (
    (__ \ "guid").read[UUID] ~
    (__ \ "user" \ "handle").read[String] ~
    (__ \ "user" \ "name").readNullable[String]
  ) { (guid: UUID, handle: String, name: Option[String]) =>
    User.public(guid, handle, name)
  }

  val writes = new Writes[PublicUser] {
    def writes(user: PublicUser): JsValue = {
      Json.obj(
        "guid" -> user.guid,
        "user" -> Json.obj(
          "handle" -> user.handle,
          "name" -> user.name
        )
      )
    }
  }

}
