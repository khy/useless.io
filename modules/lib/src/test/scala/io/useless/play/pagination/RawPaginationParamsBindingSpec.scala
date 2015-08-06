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
      RawPaginationParamsBinding.default.bind(request).toSuccess.value.limit mustBe Some(5)
    }

    "fail if 'p.limit' is specified, but non-numeric" in {
      val request = FakeRequest("GET", "/notes?p.limit=invalid")
      val errors = RawPaginationParamsBinding.default.bind(request).toFailure.errors
      val limitError = errors("pagination.limit").head
      limitError.key mustBe ("useless.error.non-numeric")
      limitError.details("specified") mustBe ("invalid")
    }

    "extract 'p.order' parameter from a request" in {
      val request = FakeRequest("GET", "/notes?p.order=rank")
      RawPaginationParamsBinding.default.bind(request).toSuccess.value.order mustBe Some("rank")
    }

    "extract 'p.offset' parameter from a request" in {
      val request = FakeRequest("GET", "/notes?p.offset=100")
      RawPaginationParamsBinding.default.bind(request).toSuccess.value.offset mustBe Some(100)
    }

    "fail if 'p.offset' is specified, but non-numeric" in {
      val request = FakeRequest("GET", "/notes?p.offset=invalid")
      val errors = RawPaginationParamsBinding.default.bind(request).toFailure.errors
      val offsetError = errors("pagination.offset").head
      offsetError.key mustBe ("useless.error.non-numeric")
      offsetError.details("specified") mustBe("invalid")
    }

    "extract 'p.page' parameter from a request" in {
      val request = FakeRequest("GET", "/notes?p.page=2")
      RawPaginationParamsBinding.default.bind(request).toSuccess.value.page mustBe Some(2)
    }

    "fail if 'p.page' is specified, but non-numeric" in {
      val request = FakeRequest("GET", "/notes?p.page=invalid")
      val errors = RawPaginationParamsBinding.default.bind(request).toFailure.errors
      val pageErrors = errors("pagination.page").head
      pageErrors.key mustBe ("useless.error.non-numeric")
      pageErrors.details("specified") mustBe("invalid")
    }

    "extract 'p.after' parameter from a request" in {
      val request = FakeRequest("GET", "/notes?p.after=52a79894-2c1b-488d-82fe-a1c1d47038cc")
      val after = RawPaginationParamsBinding.default.bind(request).toSuccess.value.after
      after mustBe Some(UUID.fromString("52a79894-2c1b-488d-82fe-a1c1d47038cc"))
    }

    "fail if 'p.after' is specified, but is not a valid UUID" in {
      val request = FakeRequest("GET", "/notes?p.after=invalid")
      val errors = RawPaginationParamsBinding.default.bind(request).toFailure.errors
      val afterError = errors("pagination.after").head
      afterError.key mustBe ("useless.error.non-uuid")
      afterError.details("specified") mustBe("invalid")
    }

  }

}
