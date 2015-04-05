package io.useless.play.pagination

import java.util.UUID
import org.scalatest._
import play.api.test.FakeRequest

class RawPaginationParamsBindingSpec
  extends WordSpec
  with MustMatchers
{

  "RawPaginationParamsBinding.default.bind" when {

    "extract 'p.limit' parameter from a request" in {
      val request = FakeRequest("GET", "/notes?p.limit=5")
      RawPaginationParamsBinding.default.bind(request).right.get.limit mustBe Some(5)
    }

    "fail if 'p.limit' is specified, but non-numeric" in {
      val request = FakeRequest("GET", "/notes?p.limit=invalid")
      val error = RawPaginationParamsBinding.default.bind(request).left.get
      error.key mustBe ("pagination.non-numeric-limit")
      error.details("specified") mustBe("invalid")
    }

    "extract 'p.order' parameter from a request" in {
      val request = FakeRequest("GET", "/notes?p.order=rank")
      RawPaginationParamsBinding.default.bind(request).right.get.order mustBe Some("rank")
    }

    "extract 'p.offset' parameter from a request" in {
      val request = FakeRequest("GET", "/notes?p.offset=100")
      RawPaginationParamsBinding.default.bind(request).right.get.offset mustBe Some(100)
    }

    "fail if 'p.offset' is specified, but non-numeric" in {
      val request = FakeRequest("GET", "/notes?p.offset=invalid")
      val error = RawPaginationParamsBinding.default.bind(request).left.get
      error.key mustBe ("pagination.non-numeric-offset")
      error.details("specified") mustBe("invalid")
    }

    "extract 'p.page' parameter from a request" in {
      val request = FakeRequest("GET", "/notes?p.page=2")
      RawPaginationParamsBinding.default.bind(request).right.get.page mustBe Some(2)
    }

    "fail if 'p.page' is specified, but non-numeric" in {
      val request = FakeRequest("GET", "/notes?p.page=invalid")
      val error = RawPaginationParamsBinding.default.bind(request).left.get
      error.key mustBe ("pagination.non-numeric-page")
      error.details("specified") mustBe("invalid")
    }

    "extract 'p.after' parameter from a request" in {
      val request = FakeRequest("GET", "/notes?p.after=52a79894-2c1b-488d-82fe-a1c1d47038cc")
      val after = RawPaginationParamsBinding.default.bind(request).right.get.after
      after mustBe Some(UUID.fromString("52a79894-2c1b-488d-82fe-a1c1d47038cc"))
    }

    "fail if 'p.after' is specified, but is not a valid UUID" in {
      val request = FakeRequest("GET", "/notes?p.after=invalid")
      val error = RawPaginationParamsBinding.default.bind(request).left.get
      error.key mustBe ("pagination.non-uuid-after")
      error.details("specified") mustBe("invalid")
    }

  }

}
