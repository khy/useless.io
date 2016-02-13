package io.useless.validation

import scala.concurrent.Future
import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import play.api.libs.concurrent.Execution.Implicits._
import play.api.test.Helpers._

class ValidationUtilSpec
  extends WordSpec
  with MustMatchers
{

  "ValidationUtil.mapFuture" must {

    "return a Future of a Validation.Success of the function's result, if the specified validation is a Validation.Success" in {
      val validation = Validation.success(1)
      val result = ValidationUtil.mapFuture(validation) { case (num) =>
        Future.successful(num.toString)
      }
      await { result }.toSuccess.value mustBe "1"
    }

    "return a Future of the Validation itself, if it is a Validation.Failure" in {
      val validation = Validation.failure("resourceKey", "is.invalid")
      val result = ValidationUtil.mapFuture(validation) { case (num) =>
        Future.successful(num.toString)
      }
      await { result }.toFailure.errors("resourceKey").head.key mustBe "is.invalid"
    }

  }

  "ValidationUtil.flatMapFuture" must {

    "return the future Validation.Success of the function's result, if the specified validation is a Validation.Success" in {
      val validation = Validation.success(1)
      val result = ValidationUtil.flatMapFuture(validation) { case (num) =>
        Future.successful(Validation.success(num.toString))
      }
      await { result }.toSuccess.value mustBe "1"
    }

    "return the future Validation.Failure of the function's result, if the specified validation is a Validation.Success" in {
      val validation = Validation.success(1)
      val result = ValidationUtil.flatMapFuture(validation) { case (num) =>
        Future.successful(Validation.failure("resourceKey", "is.invalid"))
      }
      await { result }.toFailure.errors("resourceKey").head.key mustBe "is.invalid"
    }

    "return a Future of the Validation itself, if it is a Validation.Failure" in {
      val validation = Validation.failure("resourceKey", "is.invalid")
      val result = ValidationUtil.flatMapFuture(validation) { case (num) =>
        Future.successful(Validation.success(num.toString))
      }
      await { result }.toFailure.errors("resourceKey").head.key mustBe "is.invalid"
    }

  }

}
