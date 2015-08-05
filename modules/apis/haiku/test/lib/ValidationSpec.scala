package lib.haiku

import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play.PlaySpec

class ValidationSpec extends PlaySpec {

  case class TestValue(name: String)

  "Validation.success" must {

    "return a successful validation" in {
      val value = TestValue("test")
      val validation = Validation.success(value)
      validation mustBe 'success
    }

  }

  "Validation.failure" must {

    "return a failure validation" in {
      val validation = Validation.failure(
        "resourceKey",
        "is.somehow.invalid",
        "index" -> "5"
      )

      validation mustBe 'failure
    }

  }

  "Validation#fold" must {

    "execute the first function if the validation is a failure" in {
      val validation = Validation.failure("resourceKey", "is.invalid")
      val result = validation.fold(
        failureResult => "failure",
        resource => "success"
      )
      result mustBe "failure"
    }

    "execute the second function if the validation is a success" in {
      val validation = Validation.success("success")
      val result = validation.fold(
        failureResult => "failure",
        resource => "success"
      )
      result mustBe "success"
    }

  }

  "Validation#++" must {

    "return a Validation.Success if both validations are successful" in {
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

    "return a Validation.Failure if one of the validations is a failure" in {
      val failure = Validation.failure[String]("resourceKey", "is.invalid")
      val success = Validation.success("success")
      val combined = (failure ++ success).map { case (first, second) =>
        first + second
      }
      val result = combined.toFailure.get
      result.failureResult("resourceKey").head.key mustBe "is.invalid"
    }

    "combine failure for the same key" in {
      val failure1 = Validation.failure[String]("resourceKey", "is.invalid")
      val failure2 = Validation.failure[String]("resourceKey", "is.just.wrong", "id" -> "1")
      val combined = (failure1 ++ failure2).map { case (first, second) =>
        first + second
      }
      val failureResult = combined.toFailure.get.failureResult
      failureResult("resourceKey")(0).key mustBe "is.invalid"
      failureResult("resourceKey")(1).key mustBe "is.just.wrong"
      failureResult("resourceKey")(1).details mustBe Map("id" -> "1")
    }

  }

  "Validation#map" must {

    "return Validation.Success with the new mapped value if the validation is successful" in {
      val validation = Validation.success(1)
      val _validation = validation.map(_.toString)
      _validation.toSuccess.get.value mustBe "1"
    }

    "return the existing validation if it is a a Validation.Failure" in {
      val validation = Validation.failure[Long]("resourceKey", "is.invalid")
      val _validation = validation.map(_.toString)
      _validation.toFailure.get.failureResult("resourceKey").head.key mustBe "is.invalid"
    }

  }

}
