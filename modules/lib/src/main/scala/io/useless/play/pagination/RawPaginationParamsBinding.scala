package io.useless.play.pagination

import java.util.UUID
import scala.util.{ Try, Success, Failure }
import play.api.mvc.Request
import io.useless.util.Uuid

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

    def guidParam(key: String): Option[Try[UUID]] = {
      stringParam(key).map(Uuid.parseUuid(_))
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

    val limit = request.intParam("limit").map { result =>
      result match {
        case Success(value) => Validation.success(Some(value))
        case Failure(_) => Validation.failure("pagination.limit", "useless.error.non-numeric",
          "specified" -> request.stringParam("limit").get)
      }
    }.getOrElse(Validation.success(None))

    val offset = request.intParam("offset").map { result =>
      result match {
        case Success(value) => Validation.success(Some(value))
        case Failure(_) => Validation.failure("pagination.offset", "useless.error.non-numeric",
          "specified" -> request.stringParam("offset").get)
      }
    }.getOrElse(Validation.success(None))

    val page = request.intParam("page").map { result =>
      result match {
        case Success(value) => Validation.success(Some(value))
        case Failure(_) => Validation.failure("pagination.page", "useless.error.non-numeric",
          "specified" -> request.stringParam("page").get)
      }
    }.getOrElse(Validation.success(None))

    val after = request.guidParam("after").map { result =>
      result match {
        case Success(value) => Validation.success(Some(value))
        case Failure(_) => Validation.failure("pagination.after", "useless.error.non-uuid",
          "specified" -> request.stringParam("after").get)
      }
    }.getOrElse(Validation.success(None))

    (style ++ limit ++ offset ++ page ++ after).map { case ((((style, limit), offset), page), after) =>
      val order = request.stringParam("order")
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
