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

  "Validation.combine" must {

    "return a Validation.success for value returned from the block if all validations are successful" in {
      case class Resource(id: Long, note: String, optNum: Option[Long])
      val success1 = Validation.success(1)
      val success2 = Validation.success("success")
      val success3 = Validation.success(Some(4L))
      val combined = Validation.combine(success1, success2, success3) { case (id, note, optNum) =>
        Resource(id, note, optNum)
      }
      val resource = combined.toSuccess.get.value
      resource.id  mustBe 1
      resource.note mustBe "success"
      resource.optNum mustBe Some(4)
    }

    "return a Validation.Failure if one of the validations is a failure" in {
      val failure = Validation.failure[String]("resourceKey", "is.invalid")
      val success = Validation.success("success")
      val combined = Validation.combine(failure, success) { case (first, second) =>
        first + second
      }
      val result = combined.toFailure.get
      result.failureResult("resourceKey").head.key mustBe "is.invalid"
    }

    "combine failure for the same key" in {
      val failure1 = Validation.failure[String]("resourceKey", "is.invalid")
      val failure2 = Validation.failure[String]("resourceKey", "is.just.wrong", "id" -> "1")
      val combined = Validation.combine(failure1, failure2) { case (first, second) =>
        first + second
      }
      val failureResult = combined.toFailure.get.failureResult
      failureResult("resourceKey")(0).key mustBe "is.invalid"
      failureResult("resourceKey")(1).key mustBe "is.just.wrong"
      failureResult("resourceKey")(1).details mustBe Map("id" -> "1")
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

}
