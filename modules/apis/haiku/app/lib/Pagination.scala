package lib.haiku

import java.util.UUID
import play.api.mvc.Request
import io.useless.util.Uuid

object Pagination {

  def apply[T](request: Request[T]): Pagination = new PlayPagination(request)

}

trait Pagination {

  def count: Option[Int]

  def since: Option[UUID]

  def until: Option[UUID]

}

class PlayPagination[T](request: Request[T]) extends Pagination {

  lazy val count = fromQueryString("count").map(_.toInt)

  lazy val since = guidFromQueryString("since")

  lazy val until = guidFromQueryString("until")

  private def guidFromQueryString(key: String) = fromQueryString(key).flatMap { rawGuid =>
    Uuid.parseUuid(rawGuid).toOption
  }

  private def fromQueryString[T](key: String): Option[String] = {
    request.queryString.get(key).flatMap(_.headOption)
  }

}
