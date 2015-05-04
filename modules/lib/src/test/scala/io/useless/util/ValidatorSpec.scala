package io.useless.util

import org.scalatest.FunSpec
import org.scalatest.Matchers

class ValidatorSpec
  extends FunSpec
  with    Matchers
{

  describe ("Validator.isValidEmail") {

    it ("should return true for valid emails") {
      Seq(
        "khy@me.com",
        "alexander.hamilton@gmail.com",
        "JOHN-adams@hotmail.com"
      ).foreach { email =>
        Validator.isValidEmail(email) should be (true)
      }
    }

    it ("should return false for invalid emails") {
      Seq(
        "khy",
        "george@washington",
        "jefferson.com"
      ).foreach { email =>
        Validator.isValidEmail(email) should be (false)
      }
    }

  }

  describe ("Validator.isValidHandle") {

    it ("should return true for valid handles") {
      Seq(
        "khy",
        "aarron-burr",
        "hancock123"
      ).foreach { handle =>
        Validator.isValidHandle(handle) should be (true)
      }
    }

    it ("should return false for valid handles") {
      Seq(
        "kevin hyland",
        "Aarron-BURR",
        "ben_franklin",
        "samadams!"
      ).foreach { handle =>
        Validator.isValidHandle(handle) should be (false)
      }
    }

  }

}
