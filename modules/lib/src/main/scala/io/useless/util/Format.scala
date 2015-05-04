package io.useless.util

import org.joda.time.{ DateTime, DateTimeZone }

object Format {

  def dateTime(dateTime: DateTime) = {
    dateTime.toDateTime(DateTimeZone.UTC).toString()
  }

}
