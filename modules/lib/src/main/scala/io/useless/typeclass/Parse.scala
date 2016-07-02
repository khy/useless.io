package io.useless.typeclass

import io.useless.validation.Validation

trait Parse[T] {
  def parse(raw: String): Validation[T]
}
