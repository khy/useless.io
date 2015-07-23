package lib.haiku

import java.util.UUID
import org.scalatestplus.play.PlaySpec
import play.api.test._
import play.api.test.Helpers._

class PaginationSpec extends PlaySpec {

  "Pagination#count" must {

    "return the count query paramter as an optional Int, if specified" in {
      val request = FakeRequest(GET, "/path?count=20")
      Pagination(request).count mustBe Some(20)
    }

    "return None if count is not specified as a query parameter" in {
      val request = FakeRequest(GET, "/path")
      Pagination(request).count mustBe None
    }

  }

  "Pagination#since" must {

    "return the since query parameter as an optional UUID, if specified" in {
      val request = FakeRequest(GET, "/path?since=00000000-0000-0000-0000-000000000000")
      Pagination(request).since mustBe Some(UUID.fromString("00000000-0000-0000-0000-000000000000"))
    }

    "return None if since is not specified as a query parameter" in {
      val request = FakeRequest(GET, "/path")
      Pagination(request).since mustBe None
    }

  }

  "Pagination#until" must {

    "return the until query parameter as an optional UUID, if specified" in {
      val request = FakeRequest(GET, "/path?until=00000000-0000-0000-0000-000000000000")
      Pagination(request).until mustBe Some(UUID.fromString("00000000-0000-0000-0000-000000000000"))
    }

    "return None if until is not specified as a query parameter" in {
      val request = FakeRequest(GET, "/path")
      Pagination(request).until mustBe None
    }

  }

}
