package io.useless.validation

import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import io.useless.Message

class ValidationSpec
  extends WordSpec
  with MustMatchers
{

  "Validation.success" must {

    "return a Validation.Success with the specified value" in {
      val validation = Validation.success(1)
      validation.toSuccess.value mustBe 1
    }

  }

  "Validation.failure" must {

    "return a Validation.Failure with the specified errors, using the low-level signature" in {
      val validation = Validation.failure("resourceKey", "is.invalid", "id" -> "1")
      val errors = validation.toFailure.errors
      errors("resourceKey").head.key mustBe "is.invalid"
      errors("resourceKey").head.details("id") mustBe "1"
    }

    "return a Validation.Failure with the specified errors, using Message signature" in {
      val message = Message("is.invalid", "id" -> "1")
      val validation = Validation.failure("resourceKey", message)
      val errors = validation.toFailure.errors
      errors("resourceKey").head.key mustBe "is.invalid"
      errors("resourceKey").head.details("id") mustBe "1"
    }

    "return a Validation.Failure with the specified errors, using Error signature" in {
      val message = Message("is.invalid", "id" -> "1")
      val error = Map("resourceKey" -> Seq(message))
      val validation = Validation.failure(error)
      val errors = validation.toFailure.errors
      errors("resourceKey").head.key mustBe "is.invalid"
      errors("resourceKey").head.details("id") mustBe "1"
    }

  }

  "Validation#toSuccess" must {

    "return a Validation.Success if the validation is a Validation.Success" in {
      val validation = Validation.success(1)
      validation.toSuccess.value mustBe 1
    }

    "throw a NoSuchElementException if the validation is a Validation.Failure" in {
      val validation = Validation.failure("resourceKey", "is.invalid")
      a [NoSuchElementException] must be thrownBy validation.toSuccess
    }

  }

  "Validation#toFailure" must {

    "return a Validation.Failure if the validation is a Validation.Failure" in {
      val validation = Validation.failure("resourceKey", "is.invalid")
      validation.toFailure.errors("resourceKey").head.key mustBe "is.invalid"
    }

    "throw a NoSuchElementException if the validation is a Validation.Success" in {
      val validation = Validation.success(1)
      a [NoSuchElementException] must be thrownBy validation.toFailure
    }

  }

  "Validation#map" must {

    "return Validation.Success with the result of the function, if the specified validation is a Validation.Success" in {
      val validation = Validation.success(1)
      val _validation = validation.map(_.toString)
      _validation.toSuccess.value mustBe "1"
    }

    "return the specified validation if it is a Validation.Failure" in {
      val validation = Validation.failure[Long]("resourceKey", "is.invalid")
      val _validation = validation.map(_.toString)
      _validation.toFailure.errors("resourceKey").head.key mustBe "is.invalid"
    }

  }

  "Validation#flatMap" must {

    "return Validation.Success if the validation and the function result are Validation.Success" in {
      val validation = Validation.success(1)
      val _validation = validation.flatMap { i => Validation.success(i.toString) }
      _validation.toSuccess.value mustBe "1"
    }

    "return Validation.Failure if the validation is Validation.Success, but the function returns Validation.Failure" in {
      val validation = Validation.success(1)
      val _validation = validation.flatMap { i => Validation.failure[Int]("resourceKey", "is.invalid") }
      _validation.toFailure.errors("resourceKey").head.key mustBe "is.invalid"
    }

    "return Validation.Failure if the validation and the function result are Validation.Failure" in {
      val validation = Validation.failure[Int]("resourceKey", "is.invalid")
      val _validation = validation.flatMap { i => Validation.failure[Int]("resourceKey", "is.also.invalid") }
      _validation.toFailure.errors("resourceKey").head.key mustBe "is.invalid"
    }

    "return Validation.Failure if the validation is Validation.Failure, but the function returns Validation.Success" in {
      val validation = Validation.failure[Int]("resourceKey", "is.invalid")
      val _validation = validation.flatMap { i => Validation.success(i.toString) }
      _validation.toFailure.errors("resourceKey").head.key mustBe "is.invalid"
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

      val resource = combined.toSuccess.value
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

      val errors = combined.toFailure.errors
      errors("resourceKey").head.key mustBe "is.invalid"
    }

    "return a Validation.Failure that combines the errors of any specified Validation.Failure" in {
      val success = Validation.success(1)
      val failure1 = Validation.failure[Long]("resourceKey", "is.invalid")
      val failure2 = Validation.failure[Long]("resourceKey", "is.just.wrong", "id" -> "1")
      val failure3 = Validation.failure[Long]("otherResourceKey", "is.also.wrong", "id" -> "2")
      val combined = (success ++ failure1 ++ failure2 ++ failure3).map { case (((first, second), third), fourth) =>
        first + second + third + fourth
      }

      val errors = combined.toFailure.errors
      errors("resourceKey")(0).key mustBe "is.invalid"
      errors("resourceKey")(1).key mustBe "is.just.wrong"
      errors("resourceKey")(1).details mustBe Map("id" -> "1")
      errors("otherResourceKey")(0).key mustBe "is.also.wrong"
      errors("otherResourceKey")(0).details mustBe Map("id" -> "2")
    }

  }

}
