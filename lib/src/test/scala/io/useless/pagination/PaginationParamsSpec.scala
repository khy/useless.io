package io.useless.pagination

import java.util.UUID
import org.scalatest._

class PaginationParamsSpec
  extends WordSpec
  with MustMatchers
{

  "PaginationParams.build" must {

    "use the default limit if none is specified" in {
      val raw = RawPaginationParams()
      PaginationParams.build(raw).right.get.limit mustBe (20)
    }

    "fail if the limit is negative" in {
      val raw = RawPaginationParams(limit = Some(-1))
      val error = PaginationParams.build(raw).left.get
      error.key mustBe ("pagination.non-positive-limit")
      error.details("specified") mustBe "-1"
    }

    "fail if the limit is zero" in {
      val raw = RawPaginationParams(limit = Some(0))
      val error = PaginationParams.build(raw).left.get
      error.key mustBe ("pagination.non-positive-limit")
      error.details("specified") mustBe "0"
    }

    "fail if the limit is above the maximum limit" in {
      val raw = RawPaginationParams(limit = Some(101))
      val error = PaginationParams.build(raw).left.get
      error.key mustBe ("pagination.limit-exceeds-maximum")
      error.details("specified") mustBe "101"
      error.details("maximum") mustBe "100"
    }

    "use the specified limit" in {
      val raw = RawPaginationParams(limit = Some(40))
      PaginationParams.build(raw).right.get.limit mustBe (40)
    }

    "use the default order if none is specified" in {
      val raw = RawPaginationParams()
      val params = PaginationParams.build(raw).right.get.asInstanceOf[OffsetBasedPaginationParams]
      params.order mustBe ("created_at")
    }

    "use the specified order" in {
      val config = PaginationParams.defaultPaginationConfig.copy(
        validOrders = Seq("rank", "created_at")
      )
      val raw = RawPaginationParams(order = Some("rank"))
      val params = PaginationParams.build(raw, config).right.get.asInstanceOf[OffsetBasedPaginationParams]
      params.order mustBe ("rank")
    }

    "fail if the specified order is not valid" in {
      val config = PaginationParams.defaultPaginationConfig.copy(
        validOrders = Seq("rank", "created_at")
      )
      val raw = RawPaginationParams(order = Some("votes"))
      val error = PaginationParams.build(raw, config).left.get
      error.key mustBe ("pagination.invalid-order")
      error.details("specified") mustBe ("votes")
      error.details("valid") mustBe ("'created_at', 'rank'")
    }

    "calculate the offset for the specified page" in {
      val raw = RawPaginationParams(page = Some(3), limit = Some(5))
      val params = PaginationParams.build(raw).right.get.asInstanceOf[OffsetBasedPaginationParams]
      params.offset mustBe (10)
    }

    "prefer page over offset" in {
      val raw = RawPaginationParams(page = Some(3), limit = Some(5), offset = Some(20))
      val params = PaginationParams.build(raw).right.get.asInstanceOf[OffsetBasedPaginationParams]
      params.offset mustBe (10)
    }

    "fail if the page is negative" in {
      val raw = RawPaginationParams(page = Some(-1))
      val error = PaginationParams.build(raw).left.get
      error.key mustBe ("pagination.non-positive-page")
      error.details("specified") mustBe ("-1")
    }

    "fail if the page is zero" in {
      val raw = RawPaginationParams(page = Some(0))
      val error = PaginationParams.build(raw).left.get
      error.key mustBe ("pagination.non-positive-page")
      error.details("specified") mustBe ("0")
    }

    "use the default offset if neither offset nor page is specified" in {
      val raw = RawPaginationParams()
      val params = PaginationParams.build(raw).right.get.asInstanceOf[OffsetBasedPaginationParams]
      params.offset mustBe (0)
    }

    "fail if the offset is negative" in {
      val raw = RawPaginationParams(offset = Some(-1))
      val error = PaginationParams.build(raw).left.get
      error.key mustBe ("pagination.negative-offset")
      error.details("specified") mustBe ("-1")
    }

    "use the after parameter, if specified" in {
      val guid = UUID.randomUUID
      val raw = RawPaginationParams(after = Some(guid))
      val params = PaginationParams.build(raw).right.get.asInstanceOf[PrecedenceBasedPaginationParams]
      params.after mustBe Some(guid)
    }

    "prefer page over after" in {
      val raw = RawPaginationParams(page = Some(4), after = Some(UUID.randomUUID))
      val params = PaginationParams.build(raw).right.get.asInstanceOf[OffsetBasedPaginationParams]
      params.offset mustBe (60)
    }

  }

}
