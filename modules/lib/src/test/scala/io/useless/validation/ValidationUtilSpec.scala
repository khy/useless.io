package io.useless.validation

import scala.concurrent.Future
import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import play.api.libs.concurrent.Execution.Implicits._
import play.api.test.Helpers._

import ValidationTestHelper._

class ValidationUtilSpec
  extends WordSpec
  with MustMatchers
{

  "ValidationUtil.sequence" must {

    "return a Validation.Success of the contained sequence if all Validations are successes" in {
      val validation1 = Validation.success(1)
      val validation2 = Validation.success(2)
      val validation3 = Validation.success(3)

      val validations = Seq(validation1, validation2, validation3)
      val result = ValidationUtil.sequence(validations)
      result.toSuccess.value mustBe Seq(1,2,3)
    }

    "return a Validation.Failure with any failures included in the sequence" in {
      val validation1 = Validation.success(1)
      val validation2 = Validation.failure("key2", "is.invalid")
      val validation3 = Validation.failure("key3", "is.also.invalid")

      val validations = Seq(validation1, validation2, validation3)
      val result = ValidationUtil.sequence(validations)
      val errors = result.toFailure.errors
      errors.getMessages("key2").head.key mustBe "is.invalid"
      errors.getMessages("key3").head.key mustBe "is.also.invalid"
    }

  }

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
      await { result }.toFailure.errors.getMessages("resourceKey").head.key mustBe "is.invalid"
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
      await { result }.toFailure.errors.getMessages("resourceKey").head.key mustBe "is.invalid"
    }

    "return a Future of the Validation itself, if it is a Validation.Failure" in {
      val validation = Validation.failure("resourceKey", "is.invalid")
      val result = ValidationUtil.flatMapFuture(validation) { case (num) =>
        Future.successful(Validation.success(num.toString))
      }
      await { result }.toFailure.errors.getMessages("resourceKey").head.key mustBe "is.invalid"
    }

  }

}
