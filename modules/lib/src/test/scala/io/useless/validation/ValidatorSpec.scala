package io.useless.validation

import org.scalatest.WordSpec
import org.scalatest.MustMatchers

import ValidationTestHelper._

class ValidatorSpec
  extends WordSpec
  with MustMatchers
{

  "Validator.int" must {

    "return a Validation.Success if the value is an integer" in {
      val validation = Validator.int("1", Some("key"))
      validation.toSuccess.value mustBe 1
    }

    "return a Validation.Success if the value is zero" in {
      val validation = Validator.int("0", Some("key"))
      validation.toSuccess.value mustBe 0
    }

    "return a Validation.Success if the value is a negative integer" in {
      val validation = Validator.int("-1", Some("key"))
      validation.toSuccess.value mustBe -1
    }

    "return a Validation.Failure if the value is non-numeric" in {
      val validation = Validator.int("non-numeric", Some("key"))
      val error = validation.toFailure.errors.getMessages("key").head
      error.key mustBe "useless.error.nonInt"
      error.details("specified") mustBe "non-numeric"
    }

    "return a Validation.Failure if the value is numeric, but has a fractional part" in {
      val validation = Validator.int("1.5", Some("key"))
      val error = validation.toFailure.errors.getMessages("key").head
      error.key mustBe "useless.error.nonInt"
      error.details("specified") mustBe "1.5"
    }

  }

}
