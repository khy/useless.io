package io.useless.play.pagination

import java.util.UUID
import scala.util.{ Try, Success, Failure }
import play.api.mvc.Request

import io.useless.Message
import io.useless.http.UrlUtil
import io.useless.pagination._
import io.useless.validation._

object RawPaginationParamsBinding {

  def default = new PrefixedRawPaginationParamsBinding(prefix = "p")

}

trait RawPaginationParamsBinding {

  def bind(request: Request[_]): Validation[RawPaginationParams]

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

  }

  def bind(
    request: Request[_]
  ): Validation[RawPaginationParams] = {
    val style: Validation[Option[PaginationStyle]] = request.stringParam("style").map { style =>
      style match {
        case "page" => Validation.success(Some(PageBasedPagination))
        case "precedence" => Validation.success(Some(PrecedenceBasedPagination))
        case "offset" => Validation.success(Some(OffsetBasedPagination))
        case other => Validation.failure("pagination.style", "useless.error.invalid-value",
          "specified" -> other, "valid" -> "'page', 'precedence', 'offset'")
      }
    }.getOrElse(Validation.success(None))

    val limit = request.stringParam("limit").map { limit =>
      Validator.int(limit, Some("pagination.limit")).map(Some(_))
    }.getOrElse(Validation.success(None))

    val offset = request.stringParam("offset").map { offset =>
      Validator.int(offset, Some("pagination.offset")).map(Some(_))
    }.getOrElse(Validation.success(None))

    val page = request.intParam("page").map { result =>
      result match {
        case Success(value) => Validation.success(Some(value))
        case Failure(_) => Validation.failure("pagination.page", "useless.error.non-numeric",
          "specified" -> request.stringParam("page").get)
      }
    }.getOrElse(Validation.success(None))

    (style ++ limit ++ offset ++ page).map { case (((style, limit), offset), page) =>
      val order = request.stringParam("order")
      val after = request.stringParam("after")
      RawPaginationParams(style, limit, order, offset, page, after)
    }
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
