package io.useless.accesstoken

import org.scalatest.FunSpec
import org.scalatest.Matchers

class ScopeSpec
  extends FunSpec
  with    Matchers
{

  describe ("Scope.apply") {

    it ("should parse a string without a context correctly") {
      val scope = Scope("read")
      scope.key should be ("read")
      scope.context should be (None)
    }

    it ("should parse a string with a context correctly") {
      val scope = Scope("haiku/read")
      scope.key should be ("read")
      scope.context should be (Some("haiku"))
    }

  }

  describe ("Scope.unapply") {

    it ("should return a string without a context, if none is specified") {
      val scope = Scope("read")
      Scope.unapply(scope) should be (Some("read"))
    }

    it ("should return a string with a context, if one is specified") {
      val scope = Scope("haiku/read")
      Scope.unapply(scope) should be (Some("haiku/read"))
    }

  }

  describe ("Scope#toString") {

    it ("should return a string without a context, if none is specified") {
      val scope = Scope("read")
      scope.toString should be ("read")
    }

    it ("should return a string with a context, if one is specified") {
      val scope = Scope("haiku/read")
      scope.toString should be ("haiku/read")
    }

  }

  describe ("Scope equality") {

    it ("should be true for scopes with the same key, but no context") {
      val scope1 = Scope("read")
      val scope2 = Scope("read")
      scope1 should be (scope2)
    }

    it ("should be false for scopes with different keys, but no context") {
      val scope1 = Scope("read")
      val scope2 = Scope("write")
      scope1 should not be (scope2)
    }

    it ("should be true for scopes with the same key and context") {
      val scope1 = Scope("haiku/read")
      val scope2 = Scope("haiku/read")
      scope1 should be (scope2)
    }

    it ("should be false for scopes with different keys and the same context") {
      val scope1 = Scope("haiku/read")
      val scope2 = Scope("haiku/write")
      scope1 should not be (scope2)
    }

    it ("should be false for scopes with the same key, but different contexts") {
      val scope1 = Scope("haiku/read")
      val scope2 = Scope("sidebar/read")
      scope1 should not be (scope2)
    }

    it ("should work with intersection") {
      val scope1 = Scope("read")
      val scope2a = Scope("write")
      val scope2b = Scope("write")
      val scope3 = Scope("delete")
      Seq(scope1, scope2a).intersect(Seq(scope2b, scope3)) should be (Seq(scope2a))
    }

  }

}
