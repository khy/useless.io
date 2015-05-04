package io.useless.play.filter

import scala.concurrent.Future
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._

object RequestTimeFilter {

  val DefaultHeader = "X-Request-Time"

}

class RequestTimeFilter(
  header: String = RequestTimeFilter.DefaultHeader
) extends Filter {

  override def apply(next: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {
    val start = System.currentTimeMillis

    next(request).map { result =>
      val duration = System.currentTimeMillis - start
      result.withHeaders(header -> duration.toString)
    }
  }

}
