package io.useless.http

import org.scalatest.{FunSpec, Matchers}

class UrlUtilSpec
  extends FunSpec
  with    Matchers
{

  describe ("UrlUtil.createQueryString") {

    it ("should produce an HTTP query string from a sequence of String pairs") {
      val pairs = Map("abc" -> Seq("123", "789"), "def" -> Seq("456"))
      val queryString = UrlUtil.createQueryString(pairs)
      queryString should be ("abc=123&abc=789&def=456")
    }

  }

  describe ("UrlUtil.appendQueryString") {

    it ("should append the query string with an ampersand if one already exists in base") {
      val base = "http://core.useless.io/accounts?abc=def"
      val queryString = "123=456"
      val url = UrlUtil.appendQueryString(base, queryString)
      url should be ("http://core.useless.io/accounts?abc=def&123=456")
    }

    it ("should append the query string with a question mark if there isn't one already in base") {
      val base = "http://core.useless.io/accounts"
      val queryString = "123=456"
      val url = UrlUtil.appendQueryString(base, queryString)
      url should be ("http://core.useless.io/accounts?123=456")
    }

  }

}
