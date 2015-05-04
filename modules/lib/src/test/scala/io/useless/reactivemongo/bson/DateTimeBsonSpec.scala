package io.useless.reactivemongo.bson

import org.scalatest.FunSpec
import org.scalatest.Matchers

import org.joda.time.DateTime
import reactivemongo.bson.BSONDocument

class DateTimeBsonSpec
  extends FunSpec
  with    Matchers
{

  import DateTimeBson._

  describe ("DateTimeBson") {

    it ("should serialize and deserialize DateTimes seamlessly") {
      val dateTime = DateTime.now
      val bsonDocument = BSONDocument("date_time" -> dateTime)
      bsonDocument.getAs[DateTime]("date_time") should be (Some(dateTime))
    }

  }

}
