package io.useless.typeclass

import java.util.UUID

trait Identify[T] {
  def identify(a: T): String
}

object Identify {

  import scala.language.reflectiveCalls
  implicit def guidIdentify[T <: { def guid: UUID }] = new Identify[T] {
    def identify(a: T) = a.guid.toString
  }

}
