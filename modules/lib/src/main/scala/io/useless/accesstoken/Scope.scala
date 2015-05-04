package io.useless.accesstoken

trait Scope {

  def key: String

  def context: Option[String]

  override def toString = context.map(_ + "/").getOrElse("") + key

  override def equals(other: Any) = other match { 
    case that: Scope => this.toString == that.toString
    case _ => false 
  }

  override def hashCode = 41 * (41 + key.hashCode) + context.hashCode

}

object Scope {

  def apply(value: String): Scope = value.split("/") match {
    case Array(context: String, value: String) => new BaseScope(value, Some(context))
    case _ => new BaseScope(value, None)
  }

  def unapply(scope: Scope): Option[String] = Some(scope.toString)

}

class BaseScope(
  val key: String,
  val context: Option[String]
) extends Scope
