package io.useless.play.json

import org.scalatest.FunSpec
import org.scalatest.Matchers

import java.util.UUID
import play.api.libs.json.{ Json, JsString, JsError }

class UuidJsonSpec
  extends FunSpec
  with    Matchers
{

  import UuidJson._

  describe ("UuidJson serialization / deserialization") {

    it ("should fail for an invalid UUID") {
      val badUuidJson = JsString("invalid-UUID")
      assert(
        Json.fromJson[UUID](badUuidJson).isInstanceOf[JsError],
        "Expected parse failure for 'invalid-UUID'"
      )
    }

    it ("should successfully parse a valid UUID") {
      val goodUuid = UUID.randomUUID
      val goodUuidJson = JsString(goodUuid.toString)
      Json.fromJson[UUID](goodUuidJson).get should be (goodUuid)
    }

  }

}
