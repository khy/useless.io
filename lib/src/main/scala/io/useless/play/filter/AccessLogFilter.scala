package io.useless.play.filter

import scala.concurrent.Future
import play.api.mvc._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._

class AccessLogFilter(
  override protected val requestTimeHeader: String = RequestTimeFilter.DefaultHeader
) extends AbstractAccessLogFilter {

  def log(entry: String) = Logger.info(entry)

}

trait AbstractAccessLogFilter extends Filter {

  protected def requestTimeHeader: String

  protected def log(entry: String): Unit

  override def apply(next: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {
    next(request).map { result =>
      val requestInfo = s"${request.method} ${request.uri}"
      val resultInfo = s"${result.header.status}"
      val durationInfo = result.header.headers.get(requestTimeHeader).map { duration =>
        s"${duration}ms"
      }.getOrElse("no duration information")
      log(s"${requestInfo} -> ${resultInfo} (${durationInfo})")
      result
    }
  }

}
