package io.useless.play.http

import java.util.UUID
import org.scalatest.{WordSpec, MustMatchers}
import play.api.test.Helpers._
import play.api.test.FakeRequest
import org.joda.time.DateTime

class LooseQueryStringUtilSpec
  extends WordSpec
  with MustMatchers
{

  import LooseQueryStringUtil.RichQueryStringRequest

  "RichQueryStringRequest#seqString" must {

    "return a Seq of Int values for the specified key" in {
      val request = FakeRequest(GET, "/index?key=abc&key=def")
      request.richQueryString.seqString("key") mustBe Some(Seq("abc", "def"))
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?key=abc,def&key=ghi,jkl")
      request.richQueryString.seqString("key", delim = Some(",")) mustBe Some(Seq("abc", "def", "ghi", "jkl"))
    }

  }

  "RichQueryStringRequest#string" must {

    "return the first value for the specified key" in {
      val request = FakeRequest(GET, "/index?key=abc&key=def")
      request.richQueryString.string("key") mustBe Some("abc")
    }

  }

  "RichQueryStringRequest#seqInt" must {

    "return a Seq of Int values for the specified key" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.richQueryString.seqInt("id") mustBe Some(Seq(1, 2))
    }

    "silently ignore values for the specified key that cannot parse to Int" in {
      val request = FakeRequest(GET, "/index?id=1&id=hi")
      request.richQueryString.seqInt("id") mustBe Some(Seq(1))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.richQueryString.seqInt("uuid") mustBe None
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?id=1,2&id=3,4,jah")
      request.richQueryString.seqInt("id", delim = Some(",")) mustBe Some(Seq(1,2,3,4))
    }

  }

  "RichQueryStringRequest#int" must {

    "return the first Int value for the specified key" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.richQueryString.int("id") mustBe Some(1)
    }

    "silently ignore values for the specified key that cannot parse to Int" in {
      val request = FakeRequest(GET, "/index?id=hi&id=2")
      request.richQueryString.int("id") mustBe Some(2)
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.richQueryString.int("uuid") mustBe None
    }

  }

  "RichQueryStringRequest#seqLong" must {

    "return a Seq of Long values for the specified key" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.richQueryString.seqLong("id") mustBe Some(Seq(1L, 2L))
    }

    "silently ignore values for the specified key that cannot parse to Long" in {
      val request = FakeRequest(GET, "/index?id=1&id=hi")
      request.richQueryString.seqLong("id") mustBe Some(Seq(1L))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.richQueryString.seqLong("uuid") mustBe None
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?id=1,2&id=3,4,jah")
      request.richQueryString.seqLong("id", delim = Some(",")) mustBe Some(Seq(1L,2L,3L,4L))
    }

  }

  "RichQueryStringRequest#long" must {

    "return the first Long value for the specified key" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.richQueryString.long("id") mustBe Some(1L)
    }

    "silently ignore values for the specified key that cannot parse to Long" in {
      val request = FakeRequest(GET, "/index?id=hi&id=2")
      request.richQueryString.long("id") mustBe Some(2L)
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.richQueryString.long("uuid") mustBe None
    }

  }

  "RichQueryStringRequest#seqUuid" must {

    "return a Seq of UUID values for the specified key" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000&uuid=11111111-1111-1111-1111-111111111111")
      request.richQueryString.seqUuid("uuid") mustBe Some(Seq(
        UUID.fromString("00000000-0000-0000-0000-000000000000"),
        UUID.fromString("11111111-1111-1111-1111-111111111111")
      ))
    }

    "silently ignore values for the specified key that cannot parse to UUID" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000&uuid=hi")
      request.richQueryString.seqUuid("uuid") mustBe Some(Seq(UUID.fromString("00000000-0000-0000-0000-000000000000")))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000&uuid=11111111-1111-1111-1111-111111111111")
      request.richQueryString.seqUuid("id") mustBe None
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000,11111111-1111-1111-1111-111111111111&uuid=22222222-2222-2222-2222-222222222222,jah")
      request.richQueryString.seqUuid("uuid", delim = Some(",")) mustBe Some(Seq(
        UUID.fromString("00000000-0000-0000-0000-000000000000"),
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        UUID.fromString("22222222-2222-2222-2222-222222222222")
      ))
    }

  }

  "RichQueryStringRequest#uuid" must {

    "return the first UUID value for the specified key" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000&uuid=11111111-1111-1111-1111-111111111111")
      request.richQueryString.uuid("uuid") mustBe Some(UUID.fromString("00000000-0000-0000-0000-000000000000"))
    }

    "silently ignore values for the specified key that cannot parse to UUID" in {
      val request = FakeRequest(GET, "/index?uuid=hi&uuid=11111111-1111-1111-1111-111111111111")
      request.richQueryString.uuid("uuid") mustBe Some(UUID.fromString("11111111-1111-1111-1111-111111111111"))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000&uuid=11111111-1111-1111-1111-111111111111")
      request.richQueryString.uuid("id") mustBe None
    }

  }

  "RichQueryStringRequest#seqDateTime" must {

    "return a Seq of DateTime values for the specified key" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00&time=2016-02-01T00:00:00.000-05:00")
      request.richQueryString.seqDateTime("time") mustBe Some(Seq(
        DateTime.parse("2016-01-01T00:00:00.000-05:00"),
        DateTime.parse("2016-02-01T00:00:00.000-05:00")
      ))
    }

    "silently ignore values for the specified key that cannot parse to DateTime" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00&id=hi")
      request.richQueryString.seqDateTime("time") mustBe Some(Seq(DateTime.parse("2016-01-01T00:00:00.000-05:00")))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00&time=2016-02-01T00:00:00.000-05:00")
      request.richQueryString.seqDateTime("date") mustBe None
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00,2016-02-01T00:00:00.000-05:00&time=2016-03-01T00:00:00.000-05:00,jah")
      request.richQueryString.seqDateTime("time", delim = Some(",")) mustBe Some(Seq(
        DateTime.parse("2016-01-01T00:00:00.000-05:00"),
        DateTime.parse("2016-02-01T00:00:00.000-05:00"),
        DateTime.parse("2016-03-01T00:00:00.000-05:00")
      ))
    }

  }

  "RichQueryStringRequest#dateTime" must {

    "return the first DateTime value for the specified key" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00&time=2016-02-01T00:00:00.000-05:00")
      request.richQueryString.dateTime("time") mustBe Some(DateTime.parse("2016-01-01T00:00:00.000-05:00"))
    }

    "silently ignore values for the specified key that cannot parse to DateTime" in {
      val request = FakeRequest(GET, "/index?time=hi&time=2016-02-01T00:00:00.000-05:00")
      request.richQueryString.dateTime("time") mustBe Some(DateTime.parse("2016-02-01T00:00:00.000-05:00"))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00&time=2016-02-01T00:00:00.000-05:00")
      request.richQueryString.dateTime("date") mustBe None
    }

  }

  "RichQueryStringRequest#boolean" must {

    "return the first Boolean value for the specified key" in {
      val request = FakeRequest(GET, "/index?include=true&include=false")
      request.richQueryString.boolean("include") mustBe Some(true)
    }

    "silently ignore values for the specified key that cannot parse to Boolean" in {
      val request = FakeRequest(GET, "/index?include=hi&include=false")
      request.richQueryString.boolean("include") mustBe Some(false)
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?include=true&include=false")
      request.richQueryString.boolean("exclude") mustBe None
    }

  }

}
