package io.useless.play.pagination

import scala.concurrent.Future
import play.api.mvc._
import play.api.libs.json.{ Json, Writes }

import io.useless.http.UrlUtil
import io.useless.http.LinkHeader
import io.useless.pagination._
import io.useless.play.json.MessageJson._

trait PaginationController {

  self: Controller =>

  val rawPaginationParamsBinding: RawPaginationParamsBinding =
    RawPaginationParamsBinding.default

  def withRawPaginationParams(
    block: RawPaginationParams => Result
  )(
    implicit request: Request[_]
  ): Result = {
    rawPaginationParamsBinding.bind(request).fold(
      error => Conflict(Json.toJson(error)),
      paginationParams => block(paginationParams)
    )
  }

  def withRawPaginationParams(
    block: RawPaginationParams => Future[Result]
  )(
    implicit request: Request[_]
  ): Future[Result] = {
    rawPaginationParamsBinding.bind(request).fold(
      error => Future.successful(Conflict(Json.toJson(error))),
      paginationParams => block(paginationParams)
    )
  }

  def paginatedResult[T](
    call: Call,
    result: PaginatedResult[T]
  )(
    implicit request: Request[_],
    writes: Writes[T]
  ): Result = {
    var linkValues = Seq.empty[LinkHeader.LinkValue]

    def addLinkValue(relation: String, optParams: Option[RawPaginationParams]) = {
      optParams.foreach { params =>
        val paginationParams = rawPaginationParamsBinding.unbind(params)
        val queryString = request.queryString ++ paginationParams
        val url = UrlUtil.appendQueryString(call.absoluteURL(), UrlUtil.createQueryString(queryString))
        linkValues = linkValues :+ LinkHeader.LinkValue(relation, url)
      }
    }

    addLinkValue("first", result.first)
    addLinkValue("previous", result.previous)
    addLinkValue("next", result.next)
    addLinkValue("last", result.last)

    Ok(Json.toJson(result.items)).withHeaders(
      "Link" -> LinkHeader.build(linkValues)
    )
  }

}
