package io.useless.play.json.account

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._

import io.useless.account.{ Api, PublicApi }
import io.useless.play.json.UuidJson._

object PublicApiJson {

  val reads: Reads[PublicApi] = (
    (__ \ "guid").read[UUID] ~
    (__ \ "api" \ "key").read[String]
  ) { (guid: UUID, key: String) =>
    Api.public(guid, key)
  }

  val writes = new Writes[PublicApi] {
    def writes(api: PublicApi): JsValue = {
      Json.obj(
        "guid" -> api.guid,
        "api" -> Json.obj(
          "key" -> api.key
        )
      )
    }
  }

}
