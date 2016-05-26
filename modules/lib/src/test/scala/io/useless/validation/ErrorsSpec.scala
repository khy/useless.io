package io.useless.validation

import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import io.useless.Message

import ValidationTestHelper._

class ErrorsSpec
  extends WordSpec
  with MustMatchers
{

  "Errors.combine" must {

    "combine two Errors lists like a Map" in {
      val a = Seq(
        Errors.attribute("attribute", Seq(Message("too-big", "a" -> "1"))),
        Errors.attribute("attribute-a", Seq(Message("unbearable", "b" -> "2"))),
        Errors.scalar(Seq(Message("uninspired", "c" -> "3")))
      )
      val b = Seq(
        Errors.attribute("attribute", Seq(Message("too-red", "d" -> "4"))),
        Errors.attribute("attribute-b", Seq(Message("cold", "e" -> "5"))),
        Errors.scalar(Seq(Message("lacking", "f" -> "6")))
      )

      val errors = Errors.combine(a, b)
      errors.length mustBe 4
      errors.getMessages("attribute")(0).key mustBe "too-big"
      errors.getMessages("attribute")(1).key mustBe "too-red"
      errors.getMessages("attribute-a")(0).key mustBe "unbearable"
      errors.getMessages("attribute-b")(0).key mustBe "cold"

      val generalMessages = errors.filter(_.key == None).head.messages
      generalMessages(0).key mustBe "uninspired"
      generalMessages(1).key mustBe "lacking"
    }

  }

}
