package io.useless.play.filter

import scala.concurrent.Future
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object CorsFilter extends Filter {

  private val AllowedMethods = Seq("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
  private val AllowedHeaders = Seq("Accept", "Authorization", "Content-Type")

  private val Headers = Seq(
    "Access-Control-Allow-Origin" -> "*",
    "Access-Control-Allow-Methods" -> AllowedMethods.mkString(","),
    "Access-Control-Allow-Headers" -> AllowedHeaders.mkString(",")
  )

  override def apply(next: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {
    if (request.method == "OPTIONS") {
      val result = Results.Ok.withHeaders(Headers:_*)
      Future.successful(result)
    } else {
      next(request).map { response =>
        response.withHeaders(Headers:_*)
      }
    }
  }

}
