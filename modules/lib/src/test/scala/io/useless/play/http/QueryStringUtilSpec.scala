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
      request.laxQueryString.seq[String]("key") mustBe Some(Seq("abc", "def"))
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?key=abc,def&key=ghi,jkl")
      request.laxQueryString.seq[String]("key", delim = Some(",")) mustBe Some(Seq("abc", "def", "ghi", "jkl"))
    }

  }

  "LaxQueryString#string" must {

    "return the first value for the specified key" in {
      val request = FakeRequest(GET, "/index?key=abc&key=def")
      request.laxQueryString.get[String]("key") mustBe Some("abc")
    }

  }

  "LaxQueryString#seqInt" must {

    "return a Seq of Int values for the specified key" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.laxQueryString.seq[Int]("id") mustBe Some(Seq(1, 2))
    }

    "silently ignore values for the specified key that cannot parse to Int" in {
      val request = FakeRequest(GET, "/index?id=1&id=hi")
      request.laxQueryString.seq[Int]("id") mustBe Some(Seq(1))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.laxQueryString.seq[Int]("uuid") mustBe None
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?id=1,2&id=3,4,jah")
      request.laxQueryString.seq[Int]("id", delim = Some(",")) mustBe Some(Seq(1,2,3,4))
    }

  }

  "LaxQueryString#int" must {

    "return the first Int value for the specified key" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.laxQueryString.get[Int]("id") mustBe Some(1)
    }

    "silently ignore values for the specified key that cannot parse to Int" in {
      val request = FakeRequest(GET, "/index?id=hi&id=2")
      request.laxQueryString.get[Int]("id") mustBe Some(2)
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.laxQueryString.get[Int]("uuid") mustBe None
    }

  }

  "LaxQueryString#seqLong" must {

    "return a Seq of Long values for the specified key" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.laxQueryString.seq[Long]("id") mustBe Some(Seq(1L, 2L))
    }

    "silently ignore values for the specified key that cannot parse to Long" in {
      val request = FakeRequest(GET, "/index?id=1&id=hi")
      request.laxQueryString.seq[Long]("id") mustBe Some(Seq(1L))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.laxQueryString.seq[Long]("uuid") mustBe None
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?id=1,2&id=3,4,jah")
      request.laxQueryString.seq[Long]("id", delim = Some(",")) mustBe Some(Seq(1L,2L,3L,4L))
    }

  }

  "LaxQueryString#long" must {

    "return the first Long value for the specified key" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.laxQueryString.get[Long]("id") mustBe Some(1L)
    }

    "silently ignore values for the specified key that cannot parse to Long" in {
      val request = FakeRequest(GET, "/index?id=hi&id=2")
      request.laxQueryString.get[Long]("id") mustBe Some(2L)
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.laxQueryString.get[Long]("uuid") mustBe None
    }

  }

  "LaxQueryString#seqUuid" must {

    "return a Seq of UUID values for the specified key" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000&uuid=11111111-1111-1111-1111-111111111111")
      request.laxQueryString.seq[UUID]("uuid") mustBe Some(Seq(
        UUID.fromString("00000000-0000-0000-0000-000000000000"),
        UUID.fromString("11111111-1111-1111-1111-111111111111")
      ))
    }

    "silently ignore values for the specified key that cannot parse to UUID" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000&uuid=hi")
      request.laxQueryString.seq[UUID]("uuid") mustBe Some(Seq(UUID.fromString("00000000-0000-0000-0000-000000000000")))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000&uuid=11111111-1111-1111-1111-111111111111")
      request.laxQueryString.seq[UUID]("id") mustBe None
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000,11111111-1111-1111-1111-111111111111&uuid=22222222-2222-2222-2222-222222222222,jah")
      request.laxQueryString.seq[UUID]("uuid", delim = Some(",")) mustBe Some(Seq(
        UUID.fromString("00000000-0000-0000-0000-000000000000"),
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        UUID.fromString("22222222-2222-2222-2222-222222222222")
      ))
    }

  }

  "LaxQueryString#uuid" must {

    "return the first UUID value for the specified key" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000&uuid=11111111-1111-1111-1111-111111111111")
      request.laxQueryString.get[UUID]("uuid") mustBe Some(UUID.fromString("00000000-0000-0000-0000-000000000000"))
    }

    "silently ignore values for the specified key that cannot parse to UUID" in {
      val request = FakeRequest(GET, "/index?uuid=hi&uuid=11111111-1111-1111-1111-111111111111")
      request.laxQueryString.get[UUID]("uuid") mustBe Some(UUID.fromString("11111111-1111-1111-1111-111111111111"))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000&uuid=11111111-1111-1111-1111-111111111111")
      request.laxQueryString.get[UUID]("id") mustBe None
    }

  }

  "LaxQueryString#seqDateTime" must {

    "return a Seq of DateTime values for the specified key" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00&time=2016-02-01T00:00:00.000-05:00")
      request.laxQueryString.seq[DateTime]("time") mustBe Some(Seq(
        DateTime.parse("2016-01-01T00:00:00.000-05:00"),
        DateTime.parse("2016-02-01T00:00:00.000-05:00")
      ))
    }

    "silently ignore values for the specified key that cannot parse to DateTime" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00&id=hi")
      request.laxQueryString.seq[DateTime]("time") mustBe Some(Seq(DateTime.parse("2016-01-01T00:00:00.000-05:00")))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00&time=2016-02-01T00:00:00.000-05:00")
      request.laxQueryString.seq[DateTime]("date") mustBe None
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00,2016-02-01T00:00:00.000-05:00&time=2016-03-01T00:00:00.000-05:00,jah")
      request.laxQueryString.seq[DateTime]("time", delim = Some(",")) mustBe Some(Seq(
        DateTime.parse("2016-01-01T00:00:00.000-05:00"),
        DateTime.parse("2016-02-01T00:00:00.000-05:00"),
        DateTime.parse("2016-03-01T00:00:00.000-05:00")
      ))
    }

  }

  "LaxQueryString#dateTime" must {

    "return the first DateTime value for the specified key" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00&time=2016-02-01T00:00:00.000-05:00")
      request.laxQueryString.get[DateTime]("time") mustBe Some(DateTime.parse("2016-01-01T00:00:00.000-05:00"))
    }

    "silently ignore values for the specified key that cannot parse to DateTime" in {
      val request = FakeRequest(GET, "/index?time=hi&time=2016-02-01T00:00:00.000-05:00")
      request.laxQueryString.get[DateTime]("time") mustBe Some(DateTime.parse("2016-02-01T00:00:00.000-05:00"))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00&time=2016-02-01T00:00:00.000-05:00")
      request.laxQueryString.get[DateTime]("date") mustBe None
    }

  }

  "LaxQueryString#boolean" must {

    "return the first Boolean value for the specified key" in {
      val request = FakeRequest(GET, "/index?include=true&include=false")
      request.laxQueryString.get[Boolean]("include") mustBe Some(true)
    }

    "silently ignore values for the specified key that cannot parse to Boolean" in {
      val request = FakeRequest(GET, "/index?include=hi&include=false")
      request.laxQueryString.get[Boolean]("include") mustBe Some(false)
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?include=true&include=false")
      request.laxQueryString.get[Boolean]("exclude") mustBe None
    }

  }

}
