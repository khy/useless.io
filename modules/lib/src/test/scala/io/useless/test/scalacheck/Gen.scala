package io.useless.test.scalacheck

import org.scalacheck
import scalacheck.Gen._

object Gen {

  def optionOf[T](gen: scalacheck.Gen[T]): scalacheck.Gen[Option[T]] = {
    oneOf(const(None), gen.map(Some[T](_)))
  }

  val email: scalacheck.Gen[String] = oneOf("khy@me.com", "tom.jones@gmail.com", "jerry123@hotmail.com")

  val handle: scalacheck.Gen[String] = oneOf("khy", "tom-jones", "jerry123")

}
