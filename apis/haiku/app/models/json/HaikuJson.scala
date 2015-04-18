package models.haiku.json

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.useless.play.json.UuidJson._
import io.useless.play.json.DateTimeJson._

import models.haiku.Haiku

object HaikuJson {

  implicit val haikuWrites = new Writes[Haiku] {
    def writes(haiku: Haiku): JsValue = {
      Json.obj(
        "guid" -> haiku.guid,
        "lines" -> haiku.lines,
        "created_at" -> haiku.createdAt,
        "created_by" -> Json.obj(
          "guid" -> haiku.createdBy.guid,
          "handle" -> haiku.createdBy.handle,
          "name" -> haiku.createdBy.name
        )
      )
    }
  }

}
