package io.useless.play.pagination

import java.util.UUID
import scala.util.{ Try, Success, Failure }
import play.api.mvc.Request
import io.useless.util.Uuid

import io.useless.ClientError
import io.useless.http.UrlUtil
import io.useless.pagination._

object RawPaginationParamsBinding {

  def default = new PrefixedRawPaginationParamsBinding(prefix = "p")

}

trait RawPaginationParamsBinding {

  def bind(request: Request[_]): Either[ClientError, RawPaginationParams]

  def unbind(params: RawPaginationParams): Map[String, Seq[String]]

}

class PrefixedRawPaginationParamsBinding(
  prefix: String
) extends RawPaginationParamsBinding {

  protected def makePrefixed(key: String) = prefix + "." + key

  private implicit class RichRequest(request: Request[_]) {

    def stringParam(key: String): Option[String] = {
      request.queryString.get(makePrefixed(key)).flatMap(_.headOption)
    }

    def intParam(key: String): Option[Try[Int]] = {
      stringParam(key).map { value => Try(value.toInt) }
    }

    def guidParam(key: String): Option[Try[UUID]] = {
      stringParam(key).map(Uuid.parseUuid(_))
    }

  }

  def bind(
    request: Request[_]
  ): Either[ClientError, RawPaginationParams] = {
    Right(RawPaginationParams(
      style = request.stringParam("style").map { result =>
        result match {
          case "page" => PageBasedPagination
          case "precedence" => PrecedenceBasedPagination
          case "offset" => OffsetBasedPagination
          case other => return Left(ClientError("pagination.invalid-style",
            "specified" -> other, "required" -> "page, precedence, offset"))
        }
      },
      limit = request.intParam("limit").map { result =>
        result match {
          case Success(value) => value
          case Failure(_) => return Left(ClientError("pagination.non-numeric-limit",
            "specified" -> request.stringParam("limit").get))
        }
      },
      order = request.stringParam("order"),
      offset = request.intParam("offset").map { result =>
        result match {
          case Success(value) => value
          case Failure(_) => return Left(ClientError("pagination.non-numeric-offset",
            "specified" -> request.stringParam("offset").get))
        }
      },
      page = request.intParam("page").map { result =>
        result match {
          case Success(value) => value
          case Failure(_) => return Left(ClientError("pagination.non-numeric-page",
            "specified" -> request.stringParam("page").get))
        }
      },
      after = request.guidParam("after").map { result =>
        result match {
          case Success(value) => value
          case Failure(_) => return Left(ClientError("pagination.non-uuid-after",
            "specified" -> request.stringParam("after").get))
        }
      }
    ))
  }

  def unbind(params: RawPaginationParams): Map[String, Seq[String]] = {
    var _params = Map.empty[String, Seq[String]]

    def addParam(key: String, optValue: Option[Any]) = optValue.foreach { value =>
      _params = _params + ((makePrefixed(key), Seq(value.toString)))
    }

    params.style.foreach { style =>
      val _style = style match {
        case PageBasedPagination => "page"
        case PrecedenceBasedPagination => "precedence"
        case OffsetBasedPagination => "offset"
      }

      addParam("style", Some(_style))
    }

    addParam("limit", params.limit)
    addParam("order", params.order)
    addParam("offset", params.offset)
    addParam("page", params.page)
    addParam("after", params.after)

    _params
  }

}
