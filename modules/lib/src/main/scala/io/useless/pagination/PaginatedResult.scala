package io.useless.pagination

import java.util.UUID
import scala.language.reflectiveCalls

/*
 * PaginatedResult represents the result set, plus links to other, related
 * result sets. PaginatedResult.build attempts to maintain the style of
 * pagination that was used upon request.
 */
case class PaginatedResult[T](
  items: Seq[T],
  first: Option[RawPaginationParams] = None,
  previous: Option[RawPaginationParams] = None,
  next: Option[RawPaginationParams] = None,
  last: Option[RawPaginationParams] = None
)

object PaginatedResult {

  def build[T <: { def guid: UUID }](
    items: Seq[T],
    params: PaginationParams,
    totalItems: Option[Int] = None,
    hasNext: Boolean = true
  ): PaginatedResult[T] = params match {
    case offsetParams: OffsetBasedPaginationParams => {
      PaginationParams.calculateStyle(params.raw) match {
        case OffsetBasedPagination => offsetBased(items, offsetParams, totalItems, hasNext)
        case _ => pageBased(items, offsetParams, totalItems, hasNext)
      }
    }

    case precedenceParams: PrecedenceBasedPaginationParams => {
      PaginatedResult(
        items = items,
        next = Some(precedenceParams.raw.copy(
          after = items.lastOption.map(_.guid).orElse(params.raw.after),
          style = None
        ))
      )
    }
  }

  def pageBased[T](
    items: Seq[T],
    params: OffsetBasedPaginationParams,
    totalItems: Option[Int] = None,
    hasNext: Boolean = true
  ) = {
    val page = params.raw.page.getOrElse(1)

    val optMaxPage = totalItems.map { totalItems =>
      math.ceil(totalItems / params.limit.toDouble).toInt
    }

    def newParams(page: Int) = Some(params.raw.copy(page = Some(page), offset = None))

    PaginatedResult(
      items = items,

      first = newParams(1),

      previous = if (page > 1) newParams(page - 1) else None,

      next = optMaxPage.map { maxPage =>
        if (page < maxPage) newParams(page + 1) else None
      }.getOrElse {
        if (hasNext) newParams(page + 1) else None
      },

      last = optMaxPage.flatMap { maxPage =>
        if (page < maxPage) newParams(maxPage) else None
      }
    )
  }

  def offsetBased[T](
    items: Seq[T],
    params: OffsetBasedPaginationParams,
    totalItems: Option[Int] = None,
    hasNext: Boolean = true
  ) = {
    def newParams(
      offset: Int,
      limit: Option[Int] = params.raw.limit
    ) = {
      Some(params.raw.copy(offset = Some(offset), limit = limit, page = None))
    }

    val firstPageLimit = {
      val adjustedLimit = (params.offset % params.limit)
      if (adjustedLimit > 0) Some(adjustedLimit) else params.raw.limit
    }

    PaginatedResult(
      items = items,

      first = newParams(offset = 0, limit = firstPageLimit),

      previous = if (params.offset > params.limit) {
        newParams(offset = params.offset - params.limit)
      } else if (params.offset > 0) {
        newParams(offset = 0, limit = firstPageLimit)
      } else None,

      next = totalItems.map { totalItems =>
        if ((params.offset + params.limit) < totalItems) {
          newParams(offset = params.offset + params.limit)
        } else None
      }.getOrElse {
        if (hasNext) {
          newParams(offset = params.offset + params.limit)
        } else None
      },

      last = totalItems.flatMap { totalItems =>
        val extra = totalItems % params.limit
        val finalPageLimit = if (extra > 0) extra else params.limit

        newParams(
          offset = totalItems - finalPageLimit
        )
      }
    )
  }

}
