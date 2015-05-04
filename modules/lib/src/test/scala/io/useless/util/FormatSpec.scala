package io.useless.util

import org.scalatest.FunSpec
import org.scalatest.Matchers

import org.joda.time.DateTime

class FormatSpec
  extends FunSpec
  with    Matchers
{

  describe ("Format.dateTime") {

    it ("should return the ISO 8601 format, in UTC") {
      val dateTime = new DateTime("2013-11-17T23:06:12.053-05:00")
      Format.dateTime(dateTime) should be ("2013-11-18T04:06:12.053Z")
    }

  }

}
