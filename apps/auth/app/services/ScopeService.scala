package services.auth

import io.useless.accesstoken.Scope

object ScopeService {

  def instance = new StaticScopeService(Seq(
    RichScope(
      raw = "admin",
      name = "Admin",
      description = "Full administrative access to your account. This includes " +
        "the ability to edit account information and create and delete access tokens."
    )
  ))

}

class StaticScopeService(richScopes: Seq[RichScope]) {

  def getRichScope(raw: String) = richScopes.find { _.toString == raw }.getOrElse {
    new DefaultRichScope(raw)
  }

}

object RichScope {

  def apply(raw: String, name: String, description: String) = {
    new BaseRichScope(raw, name, Some(description))
  }

}

trait RichScope extends Scope {

  def name: String

  def description: Option[String]

}

class BaseRichScope(
  raw: String,
  val name: String,
  val description: Option[String]
) extends RichScope {

  protected val underlying = Scope(raw)

  def key = underlying.key

  def context = underlying.context

}

class DefaultRichScope(
  raw: String
) extends BaseRichScope(raw, raw, None) {

  override val name = underlying.key

}
