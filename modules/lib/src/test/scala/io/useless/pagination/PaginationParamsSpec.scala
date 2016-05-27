package io.useless.pagination

import java.util.UUID
import org.scalatest._

import io.useless.Message
import io.useless.validation.{Errors, Validation}
import io.useless.validation.ValidationTestHelper._

class PaginationParamsSpec
  extends WordSpec
  with MustMatchers
{

  "PaginationParams.build" must {

    "use the default limit if none is specified" in {
      val raw = RawPaginationParams()
      PaginationParams.build(raw).toSuccess.value.limit mustBe (20)
    }

    "fail if the limit is negative" in {
      val raw = RawPaginationParams(limit = Some(-1))
      val errors = PaginationParams.build(raw).toFailure.errors
      val limitError = errors.getMessages("pagination.limit").head
      limitError.key mustBe ("useless.error.non-positive")
      limitError.details("specified") mustBe "-1"
    }

    "fail if the limit is zero" in {
      val raw = RawPaginationParams(limit = Some(0))
      val errors = PaginationParams.build(raw).toFailure.errors
      val limitError = errors.getMessages("pagination.limit").head
      limitError.key mustBe ("useless.error.non-positive")
      limitError.details("specified") mustBe "0"
    }

    "fail if the limit is above the maximum limit" in {
      val raw = RawPaginationParams(limit = Some(101))
      val errors = PaginationParams.build(raw).toFailure.errors
      val limitError = errors.getMessages("pagination.limit").head
      limitError.key mustBe ("useless.error.exceeds-maximum")
      limitError.details("specified") mustBe "101"
      limitError.details("maximum") mustBe "100"
    }

    "use the specified limit" in {
      val raw = RawPaginationParams(limit = Some(40))
      PaginationParams.build(raw).toSuccess.value.limit mustBe (40)
    }

    "use the default order if none is specified" in {
      val raw = RawPaginationParams()
      val params = PaginationParams.build(raw).toSuccess.value.asInstanceOf[OffsetBasedPaginationParams]
      params.order mustBe ("created_at")
    }

    "use the specified order" in {
      val config = PaginationParams.defaultPaginationConfig.copy(
        validOrders = Seq("rank", "created_at")
      )
      val raw = RawPaginationParams(order = Some("rank"))
      val params = PaginationParams.build(raw, config).toSuccess.value.asInstanceOf[OffsetBasedPaginationParams]
      params.order mustBe ("rank")
    }

    "fail if the specified order is not valid" in {
      val config = PaginationParams.defaultPaginationConfig.copy(
        validOrders = Seq("rank", "created_at")
      )
      val raw = RawPaginationParams(order = Some("votes"))
      val errors = PaginationParams.build(raw, config).toFailure.errors
      val orderError = errors.getMessages("pagination.order").head
      orderError.key mustBe ("useless.error.invalid-value")
      orderError.details("specified") mustBe ("votes")
      orderError.details("valid") mustBe ("'created_at', 'rank'")
    }

    "calculate the offset for the specified page" in {
      val raw = RawPaginationParams(page = Some(3), limit = Some(5))
      val params = PaginationParams.build(raw).toSuccess.value.asInstanceOf[OffsetBasedPaginationParams]
      params.offset mustBe (10)
    }

    "prefer page over offset" in {
      val raw = RawPaginationParams(page = Some(3), limit = Some(5), offset = Some(20))
      val params = PaginationParams.build(raw).toSuccess.value.asInstanceOf[OffsetBasedPaginationParams]
      params.offset mustBe (10)
    }

    "fail if the page is negative" in {
      val raw = RawPaginationParams(page = Some(-1))
      val errors = PaginationParams.build(raw).toFailure.errors
      val pageError = errors.getMessages("pagination.page").head
      pageError.key mustBe ("useless.error.non-positive")
      pageError.details("specified") mustBe ("-1")
    }

    "fail if the page is zero" in {
      val raw = RawPaginationParams(page = Some(0))
      val errors = PaginationParams.build(raw).toFailure.errors
      val pageError = errors.getMessages("pagination.page").head
      pageError.key mustBe ("useless.error.non-positive")
      pageError.details("specified") mustBe ("0")
    }

    "use the default offset if neither offset nor page is specified" in {
      val raw = RawPaginationParams()
      val params = PaginationParams.build(raw).toSuccess.value.asInstanceOf[OffsetBasedPaginationParams]
      params.offset mustBe (0)
    }

    "fail if the offset is negative" in {
      val raw = RawPaginationParams(offset = Some(-1))
      val errors = PaginationParams.build(raw).toFailure.errors
      val offsetError = errors.getMessages("pagination.offset").head
      offsetError.key mustBe ("useless.error.negative")
      offsetError.details("specified") mustBe ("-1")
    }

    "fail if the after parameter is not valid" in {
      val raw = RawPaginationParams(after = Some("123"))
      val config = PaginationParams.defaultPaginationConfig.copy(
        afterParser = (after) => after match {
          case "abc" => Validation.Success(after)
          case other => {
            val message = Message("useless.error.no-abc", "specified" -> other)
            Validation.Failure(Seq(Errors.scalar(Seq(message))))
          }
        }
      )

      val errors = PaginationParams.build(raw, config).toFailure.errors
      val offsetError = errors.getMessages("pagination.after").head
      offsetError.key mustBe ("useless.error.no-abc")
      offsetError.details("specified") mustBe ("123")
    }

    "use the after parameter, if specified" in {
      val guid = UUID.randomUUID
      val raw = RawPaginationParams(after = Some(guid.toString))
      val params = PaginationParams.build(raw).toSuccess.value.asInstanceOf[PrecedenceBasedPaginationParams]
      params.after mustBe Some(guid.toString)
    }

    "prefer page over after" in {
      val raw = RawPaginationParams(page = Some(4), after = Some(UUID.randomUUID.toString))
      val params = PaginationParams.build(raw).toSuccess.value.asInstanceOf[OffsetBasedPaginationParams]
      params.offset mustBe (60)
    }

  }

}
