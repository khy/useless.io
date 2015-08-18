package io.useless.validation

import org.scalatest.WordSpec
import org.scalatest.MustMatchers

class ValidatorSpec
  extends WordSpec
  with MustMatchers
{

  "Validator.int" must {

    "return a Validation.Success if the value is an integer" in {
      val validation = Validator.int("key", "1")
      validation.toSuccess.value mustBe 1
    }

    "return a Validation.Success if the value is zero" in {
      val validation = Validator.int("key", "0")
      validation.toSuccess.value mustBe 0
    }

    "return a Validation.Success if the value is a negative integer" in {
      val validation = Validator.int("key", "-1")
      validation.toSuccess.value mustBe -1
    }

    "return a Validation.Failure if the value is non-numeric" in {
      val validation = Validator.int("key", "non-numeric")
      val error = validation.toFailure.errors("key").head
      error.key mustBe "useless.error.nonInt"
      error.details("specified") mustBe "non-numeric"
    }

    "return a Validation.Failure if the value is numeric, but has a fractional part" in {
      val validation = Validator.int("key", "1.5")
      val error = validation.toFailure.errors("key").head
      error.key mustBe "useless.error.nonInt"
      error.details("specified") mustBe "1.5"
    }

  }

}
