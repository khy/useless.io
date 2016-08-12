package io.useless.play.json.account

import java.util.UUID
import play.api.libs.json._
import io.useless.account.{PublicUser, User}

object UserJson {

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

}
