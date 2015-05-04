package io.useless.play.json.account

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._

import io.useless.account.{ App, PublicApp }
import io.useless.play.json.UuidJson._

object PublicAppJson {

  val reads: Reads[PublicApp] = (
    (__ \ "guid").read[UUID] ~
    (__ \ "app" \ "name").read[String] ~
    (__ \ "app" \ "url").read[String]
  ) { (guid: UUID, name: String, url: String) =>
    App.public(guid, name, url)
  }

  val writes = new Writes[PublicApp] {
    def writes(app: PublicApp): JsValue = {
      Json.obj(
        "guid" -> app.guid,
        "app" -> Json.obj(
          "name" -> app.name,
          "url" -> app.url
        )
      )
    }
  }

}
