package io.useless.play.http

import java.util.UUID
import org.scalatest.{WordSpec, MustMatchers}
import play.api.test.Helpers._
import play.api.test.FakeRequest
import org.joda.time.DateTime

import io.useless.validation.Validation
import io.useless.validation.ValidationTestHelper._

class QueryStringUtilSpec
  extends WordSpec
  with MustMatchers
{

  import QueryStringUtil._

  "RichQueryString#get[String]" must {

    "return a Seq of Int values for the specified key" in {
      val request = FakeRequest(GET, "/index?key=abc&key=def")
      request.richQueryString.get[String]("key") mustBe Some(Seq("abc", "def"))
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?key=abc,def&key=ghi,jkl")
      request.richQueryString.get[String]("key", delim = Some(",")) mustBe Some(Seq("abc", "def", "ghi", "jkl"))
    }

  }

  "RichQueryString#get[Int]" must {

    "return a Seq of Int values for the specified key" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.richQueryString.get[Int]("id") mustBe Some(Seq(1, 2))
    }

    "silently ignore values for the specified key that cannot parse to Int" in {
      val request = FakeRequest(GET, "/index?id=1&id=hi")
      request.richQueryString.get[Int]("id") mustBe Some(Seq(1))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.richQueryString.get[Int]("uuid") mustBe None
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?id=1,2&id=3,4,jah")
      request.richQueryString.get[Int]("id", delim = Some(",")) mustBe Some(Seq(1,2,3,4))
    }

  }

  "RichQueryString#get[Long]" must {

    "return a Seq of Long values for the specified key" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.richQueryString.get[Long]("id") mustBe Some(Seq(1L, 2L))
    }

    "silently ignore values for the specified key that cannot parse to Long" in {
      val request = FakeRequest(GET, "/index?id=1&id=hi")
      request.richQueryString.get[Long]("id") mustBe Some(Seq(1L))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?id=1&id=2")
      request.richQueryString.get[Long]("uuid") mustBe None
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?id=1,2&id=3,4,jah")
      request.richQueryString.get[Long]("id", delim = Some(",")) mustBe Some(Seq(1L,2L,3L,4L))
    }

  }


  "RichQueryString#get[Uuid]" must {

    "return a Seq of UUID values for the specified key" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000&uuid=11111111-1111-1111-1111-111111111111")
      request.richQueryString.get[UUID]("uuid") mustBe Some(Seq(
        UUID.fromString("00000000-0000-0000-0000-000000000000"),
        UUID.fromString("11111111-1111-1111-1111-111111111111")
      ))
    }

    "silently ignore values for the specified key that cannot parse to UUID" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000&uuid=hi")
      request.richQueryString.get[UUID]("uuid") mustBe Some(Seq(UUID.fromString("00000000-0000-0000-0000-000000000000")))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000&uuid=11111111-1111-1111-1111-111111111111")
      request.richQueryString.get[UUID]("id") mustBe None
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?uuid=00000000-0000-0000-0000-000000000000,11111111-1111-1111-1111-111111111111&uuid=22222222-2222-2222-2222-222222222222,jah")
      request.richQueryString.get[UUID]("uuid", delim = Some(",")) mustBe Some(Seq(
        UUID.fromString("00000000-0000-0000-0000-000000000000"),
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        UUID.fromString("22222222-2222-2222-2222-222222222222")
      ))
    }

  }

  "RichQueryString#get[DateTime]" must {

    "return a Seq of DateTime values for the specified key" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00&time=2016-02-01T00:00:00.000-05:00")
      request.richQueryString.get[DateTime]("time") mustBe Some(Seq(
        DateTime.parse("2016-01-01T00:00:00.000-05:00"),
        DateTime.parse("2016-02-01T00:00:00.000-05:00")
      ))
    }

    "silently ignore values for the specified key that cannot parse to DateTime" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00&id=hi")
      request.richQueryString.get[DateTime]("time") mustBe Some(Seq(DateTime.parse("2016-01-01T00:00:00.000-05:00")))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00&time=2016-02-01T00:00:00.000-05:00")
      request.richQueryString.get[DateTime]("date") mustBe None
    }

    "support splitting by delimiter" in {
      val request = FakeRequest(GET, "/index?time=2016-01-01T00:00:00.000-05:00,2016-02-01T00:00:00.000-05:00&time=2016-03-01T00:00:00.000-05:00,jah")
      request.richQueryString.get[DateTime]("time", delim = Some(",")) mustBe Some(Seq(
        DateTime.parse("2016-01-01T00:00:00.000-05:00"),
        DateTime.parse("2016-02-01T00:00:00.000-05:00"),
        DateTime.parse("2016-03-01T00:00:00.000-05:00")
      ))
    }

  }

  "RichQueryString#get[Boolean]" must {

    "return a Seq of Boolean values for the specified key" in {
      val request = FakeRequest(GET, "/index?include=true&include=false")
      request.richQueryString.get[Boolean]("include") mustBe Some(Seq(true, false))
    }

    "silently ignore values for the specified key that cannot parse to Boolean" in {
      val request = FakeRequest(GET, "/index?include=hi&include=false")
      request.richQueryString.get[Boolean]("include") mustBe Some(Seq(false))
    }

    "return None if the specified key is not in the query string" in {
      val request = FakeRequest(GET, "/index?include=true&include=false")
      request.richQueryString.get[Boolean]("exclude") mustBe None
    }

  }

}
