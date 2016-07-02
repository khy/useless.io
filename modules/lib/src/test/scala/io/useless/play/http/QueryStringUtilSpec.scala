package io.useless.play.http

import java.util.UUID
import org.scalatest.{WordSpec, MustMatchers}
import play.api.test.Helpers._
import play.api.test.FakeRequest
import org.joda.time.DateTime

class QueryStringUtilSpec
  extends WordSpec
  with MustMatchers
{

  import QueryStringUtil._

  "LaxQueryString#seqString" must {

    "return a Seq of Int values for the specified key" in {
      val request = FakeRequest(GET, "/index?key=abc&key=def")
      request.laxQueryString.seqString("key") mustBe Some(Seq("abc", "def"))
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?key=abc,def&key=ghi,jkl")
      request.laxQueryString.seqString("key", delim = Some(",")) mustBe Some(Seq("abc", "def", "ghi", "jkl"))
    }

  }

  "LaxQueryString#string" must {

    "return the first value for the specified key" in {
      val request = FakeRequest(GET, "/index?key=abc&key=def")
      request.laxQueryString.string("key") mustBe Some("abc")
    }

  }

  "LaxQueryString#seqInt" must {

    "return a Seq of Int values for the specified key" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.laxQueryString.seqInt("id") mustBe Some(Seq(1, 2))
    }

    "silently ignore values for the specified key that cannot parse to Int" in {
      val request = FakeRequest(GET, "/index?id=1&id=hi")
      request.laxQueryString.seqInt("id") mustBe Some(Seq(1))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.laxQueryString.seqInt("uuid") mustBe None
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?id=1,2&id=3,4,jah")
      request.laxQueryString.seqInt("id", delim = Some(",")) mustBe Some(Seq(1,2,3,4))
    }

  }

  "LaxQueryString#int" must {

    "return the first Int value for the specified key" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.laxQueryString.int("id") mustBe Some(1)
    }

    "silently ignore values for the specified key that cannot parse to Int" in {
      val request = FakeRequest(GET, "/index?id=hi&id=2")
      request.laxQueryString.int("id") mustBe Some(2)
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.laxQueryString.int("uuid") mustBe None
    }

  }

  "LaxQueryString#seqLong" must {

    "return a Seq of Long values for the specified key" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.laxQueryString.seqLong("id") mustBe Some(Seq(1L, 2L))
    }

    "silently ignore values for the specified key that cannot parse to Long" in {
      val request = FakeRequest(GET, "/index?id=1&id=hi")
      request.laxQueryString.seqLong("id") mustBe Some(Seq(1L))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.laxQueryString.seqLong("uuid") mustBe None
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?id=1,2&id=3,4,jah")
      request.laxQueryString.seqLong("id", delim = Some(",")) mustBe Some(Seq(1L,2L,3L,4L))
    }

  }

  "LaxQueryString#long" must {

    "return the first Long value for the specified key" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.laxQueryString.long("id") mustBe Some(1L)
    }

    "silently ignore values for the specified key that cannot parse to Long" in {
      val request = FakeRequest(GET, "/index?id=hi&id=2")
      request.laxQueryString.long("id") mustBe Some(2L)
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.laxQueryString.long("uuid") mustBe None
    }

  }

  "LaxQueryString#seqUuid" must {

    "return a Seq of UUID values for the specified key" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000&uuid=11111111-1111-1111-1111-111111111111")
      request.laxQueryString.seqUuid("uuid") mustBe Some(Seq(
        UUID.fromString("00000000-0000-0000-0000-000000000000"),
        UUID.fromString("11111111-1111-1111-1111-111111111111")
      ))
    }

    "silently ignore values for the specified key that cannot parse to UUID" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000&uuid=hi")
      request.laxQueryString.seqUuid("uuid") mustBe Some(Seq(UUID.fromString("00000000-0000-0000-0000-000000000000")))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000&uuid=11111111-1111-1111-1111-111111111111")
      request.laxQueryString.seqUuid("id") mustBe None
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000,11111111-1111-1111-1111-111111111111&uuid=22222222-2222-2222-2222-222222222222,jah")
      request.laxQueryString.seqUuid("uuid", delim = Some(",")) mustBe Some(Seq(
        UUID.fromString("00000000-0000-0000-0000-000000000000"),
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        UUID.fromString("22222222-2222-2222-2222-222222222222")
      ))
    }

  }

  "LaxQueryString#uuid" must {

    "return the first UUID value for the specified key" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000&uuid=11111111-1111-1111-1111-111111111111")
      request.laxQueryString.uuid("uuid") mustBe Some(UUID.fromString("00000000-0000-0000-0000-000000000000"))
    }

    "silently ignore values for the specified key that cannot parse to UUID" in {
      val request = FakeRequest(GET, "/index?uuid=hi&uuid=11111111-1111-1111-1111-111111111111")
      request.laxQueryString.uuid("uuid") mustBe Some(UUID.fromString("11111111-1111-1111-1111-111111111111"))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000&uuid=11111111-1111-1111-1111-111111111111")
      request.laxQueryString.uuid("id") mustBe None
    }

  }

  "LaxQueryString#seqDateTime" must {

    "return a Seq of DateTime values for the specified key" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00&time=2016-02-01T00:00:00.000-05:00")
      request.laxQueryString.seqDateTime("time") mustBe Some(Seq(
        DateTime.parse("2016-01-01T00:00:00.000-05:00"),
        DateTime.parse("2016-02-01T00:00:00.000-05:00")
      ))
    }

    "silently ignore values for the specified key that cannot parse to DateTime" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00&id=hi")
      request.laxQueryString.seqDateTime("time") mustBe Some(Seq(DateTime.parse("2016-01-01T00:00:00.000-05:00")))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00&time=2016-02-01T00:00:00.000-05:00")
      request.laxQueryString.seqDateTime("date") mustBe None
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00,2016-02-01T00:00:00.000-05:00&time=2016-03-01T00:00:00.000-05:00,jah")
      request.laxQueryString.seqDateTime("time", delim = Some(",")) mustBe Some(Seq(
        DateTime.parse("2016-01-01T00:00:00.000-05:00"),
        DateTime.parse("2016-02-01T00:00:00.000-05:00"),
        DateTime.parse("2016-03-01T00:00:00.000-05:00")
      ))
    }

  }

  "LaxQueryString#dateTime" must {

    "return the first DateTime value for the specified key" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00&time=2016-02-01T00:00:00.000-05:00")
      request.laxQueryString.dateTime("time") mustBe Some(DateTime.parse("2016-01-01T00:00:00.000-05:00"))
    }

    "silently ignore values for the specified key that cannot parse to DateTime" in {
      val request = FakeRequest(GET, "/index?time=hi&time=2016-02-01T00:00:00.000-05:00")
      request.laxQueryString.dateTime("time") mustBe Some(DateTime.parse("2016-02-01T00:00:00.000-05:00"))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00&time=2016-02-01T00:00:00.000-05:00")
      request.laxQueryString.dateTime("date") mustBe None
    }

  }

  "LaxQueryString#boolean" must {

    "return the first Boolean value for the specified key" in {
      val request = FakeRequest(GET, "/index?include=true&include=false")
      request.laxQueryString.boolean("include") mustBe Some(true)
    }

    "silently ignore values for the specified key that cannot parse to Boolean" in {
      val request = FakeRequest(GET, "/index?include=hi&include=false")
      request.laxQueryString.boolean("include") mustBe Some(false)
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?include=true&include=false")
      request.laxQueryString.boolean("exclude") mustBe None
    }

  }

}
