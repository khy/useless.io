package io.useless.reactivemongo.bson

import org.joda.time.DateTime
import reactivemongo.bson._

object DateTimeBson {

  implicit object DateTimeBsonReader extends BSONReader[BSONDateTime, DateTime] {
    def read(dateTime: BSONDateTime): DateTime = new DateTime(dateTime.value)
  }

  implicit object DateTimeBsonWriter extends BSONWriter[DateTime, BSONDateTime] {
    def write(dateTime: DateTime): BSONDateTime = new BSONDateTime(dateTime.getMillis)
  }

}
