package io.useless.play.filter

import play.core.Router
import play.api.mvc.{RequestHeader, Handler}

object EmptyRouter extends Router.Routes {
  def documentation = Seq.empty
  val routes = PartialFunction.empty[RequestHeader, Handler]
  def setPrefix(prefix: String) {}
  def prefix = ""
}
