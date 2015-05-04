package io.useless.play.json

import org.scalatest.FunSpec
import org.scalatest.Matchers

import org.joda.time.{ DateTime, DateTimeZone }
import play.api.libs.json.{ Json, JsString, JsError }

class DateTimeJsonSpec
  extends FunSpec
  with    Matchers
{

  import DateTimeJson._

  describe ("DateTime serialization / deserialization") {

    it ("should fail for an invalid UUID") {
      val badDateTimeJson = JsString("invalid-DateTime")
      assert(
        Json.fromJson[DateTime](badDateTimeJson).isInstanceOf[JsError],
        "Expected parse failure for 'invalid-DateTime'"
      )
    }

    it ("should successfully parse a valid DateTime") {
      val goodDateTime = DateTime.now
      val goodDateTimeJson = Json.toJson[DateTime](goodDateTime)
      Json.fromJson[DateTime](goodDateTimeJson).get should be (goodDateTime.toDateTime(DateTimeZone.UTC))
    }

    it ("should serialize in ISO 8601 format") {
      val dateTime = new DateTime("2013-11-17T23:06:12.053-05:00")
      val dateTimeJson = Json.toJson[DateTime](dateTime)
      dateTimeJson.as[String] should be ("2013-11-18T04:06:12.053Z")
    }

  }

}
