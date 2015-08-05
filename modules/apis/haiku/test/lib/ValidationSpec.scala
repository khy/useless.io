package lib.haiku

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play.PlaySpec

class ValidationSpec extends PlaySpec {

  "Validation.success" must {

    "return a Validation.Success" in {
      val validation = Validation.success(1)
      validation.isSuccess mustBe true
    }

  }

  "Validation.failure" must {

    "return a Validation.Failure" in {
      val validation = Validation.failure("resourceKey", "is.invalid")
      validation.isFailure mustBe true
    }

  }

  "Validation.future" must {

    "return a Future of a Validation.Success of the function's result, if the specified validation is a Validation.Success" in {
      val validation = Validation.success(1)
      val result = Validation.future(validation) { case (num) =>
        Future.successful(num.toString)
      }
      await { result }.toSuccess.get.value mustBe "1"
    }

    "return a Future of the Validation itself, if it is a Validation.Failure" in {
      val validation = Validation.failure("resourceKey", "is.invalid")
      val result = Validation.future(validation) { case (num) =>
        Future.successful(num.toString)
      }
      await { result }.toFailure.get.result("resourceKey").head.key mustBe "is.invalid"
    }

  }

  "Validation#fold" must {

    "return the result of the first function if the validation is a Validation.Failure" in {
      val validation = Validation.failure("resourceKey", "is.invalid")
      val result = validation.fold(
        failureResult => "failure",
        resource => "success"
      )
      result mustBe "failure"
    }

    "return the result of the second function if the validation is a Validation.Success" in {
      val validation = Validation.success(1)
      val result = validation.fold(
        failureResult => "failure",
        resource => "success"
      )
      result mustBe "success"
    }

  }

  "Validation#++" must {

    "return a Validation.Success of the result of the function, if both validations are Validation.Success" in {
      case class Resource(one: Long, two: String, three: Option[Long])

      val success1 = Validation.success(1)
      val success2 = Validation.success("2")
      val success3 = Validation.success(Some(3L))
      val combined = (success1 ++ success2 ++ success3) map { case ((one, two), three) =>
        Resource(one, two, three)
      }

      val resource = combined.toSuccess.get.value
      resource.one mustBe 1
      resource.two mustBe "2"
      resource.three mustBe Some(3)
    }

    "return the validation that is a Validation.Failure, if there is one" in {
      val failure = Validation.failure[Long]("resourceKey", "is.invalid")
      val success = Validation.success(1)
      val combined = (failure ++ success).map { case (first, second) =>
        first + second
      }

      val result = combined.toFailure.get.result
      result("resourceKey").head.key mustBe "is.invalid"
    }

    "return a Validation.Failure that combines the results of any specified Validation.Failure" in {
      val success = Validation.success(1)
      val failure1 = Validation.failure[Long]("resourceKey", "is.invalid")
      val failure2 = Validation.failure[Long]("resourceKey", "is.just.wrong", "id" -> "1")
      val failure3 = Validation.failure[Long]("otherResourceKey", "is.also.wrong", "id" -> "2")
      val combined = (success ++ failure1 ++ failure2 ++ failure3).map { case (((first, second), third), fourth) =>
        first + second + third + fourth
      }

      val result = combined.toFailure.get.result
      result("resourceKey")(0).key mustBe "is.invalid"
      result("resourceKey")(1).key mustBe "is.just.wrong"
      result("resourceKey")(1).details mustBe Map("id" -> "1")
      result("otherResourceKey")(0).key mustBe "is.also.wrong"
      result("otherResourceKey")(0).details mustBe Map("id" -> "2")
    }

  }

  "Validation#map" must {

    "return Validation.Success with the result of the function, if the specified validation is a Validation.Success" in {
      val validation = Validation.success(1)
      val _validation = validation.map(_.toString)
      _validation.toSuccess.get.value mustBe "1"
    }

    "return the specified validation if it is a Validation.Failure" in {
      val validation = Validation.failure[Long]("resourceKey", "is.invalid")
      val _validation = validation.map(_.toString)
      _validation.toFailure.get.result("resourceKey").head.key mustBe "is.invalid"
    }

  }

}
