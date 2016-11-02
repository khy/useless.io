package io.useless

trait NamedEnum {
  def key: String
  def name: String
}

trait NamedEnumCompanion[T <: NamedEnum] {

  def values: Seq[T]

  def unknown(key: String): T

  def apply(key: String): T = {
    values.find(_.key == key).getOrElse(unknown(key))
  }

}
