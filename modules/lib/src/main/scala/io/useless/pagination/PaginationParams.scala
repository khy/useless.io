package io.useless.pagination

import java.util.UUID

import io.useless.ClientError

/*
 * Minimally-parsed pagination specifications.
 */
case class RawPaginationParams(
  style: Option[PaginationStyle] = None,
  limit: Option[Int] = None,
  order: Option[String] = None,
  offset: Option[Int] = None,
  page: Option[Int] = None,
  after: Option[UUID] = None
)

/*
 * The result of parsing a RawPaginationParams. Will either be offset-based or
 * precedence-based.
 */
sealed trait PaginationParams {

  def raw: RawPaginationParams

  def config: PaginationConfig

  def limit = raw.limit.getOrElse(config.defaultLimit)

  def order = raw.order.getOrElse(config.defaultOrder)

}

/*
 * OffsetBasedPaginationParams has an offset, of course, which is either
 * calculated from page, or just the raw offset.
 */
class OffsetBasedPaginationParams private [pagination] (
  val raw: RawPaginationParams,
  val config: PaginationConfig
) extends PaginationParams {

  def offset = raw.page.map { page =>
    ((page - 1) * limit)
  }.orElse(raw.offset).getOrElse(config.defaultOffset)

}

/*
 * PrecedenceBasedPaginationParams has an after, which signifies the resource
 * afterwhich the result set should start. This is just the raw after.
 */
class PrecedenceBasedPaginationParams private [pagination] (
  val raw: RawPaginationParams,
  val config: PaginationConfig
) extends PaginationParams {

  def after = raw.after

}

object PaginationParams {

  val defaultPaginationConfig = PaginationConfig(
    defaultStyle = PageBasedPagination,
    maxLimit = 100,
    defaultLimit = 20,
    defaultOffset = 0,
    validOrders = Seq("created_at"),
    defaultOrder = "created_at"
  )

  def calculateStyle(
    raw: RawPaginationParams,
    config: PaginationConfig = defaultPaginationConfig
  ): PaginationStyle = {
    raw.style.getOrElse {
      if (raw.page.isDefined) {
        PageBasedPagination
      } else if (raw.offset.isDefined) {
        OffsetBasedPagination
      } else if (raw.after.isDefined) {
        PrecedenceBasedPagination
      } else {
        config.defaultStyle
      }
    }
  }

  def build(
    raw: RawPaginationParams,
    config: PaginationConfig = defaultPaginationConfig
  ): Either[ClientError, PaginationParams] = {
    raw.limit.foreach { limit =>
      if (limit <= 0) {
        return Left(ClientError("pagination.non-positive-limit",
          "specified" -> limit.toString))
      } else if (limit > config.maxLimit) {
        return Left(ClientError("pagination.limit-exceeds-maximum",
          "specified" -> limit.toString, "maximum" -> config.maxLimit.toString))
      }
    }

    raw.order.foreach { order =>
      if (!config.validOrders.contains(order)) {
        val formattedValidOptions = config.validOrders.
          sortWith(_.toLowerCase < _.toLowerCase).
          map("'" + _ + "'").mkString(", ")

        return Left(ClientError("pagination.invalid-order",
          "specified" -> order, "valid" -> formattedValidOptions))
      }
    }

    raw.page.foreach { page =>
      if (page <= 0) {
        return Left(ClientError("pagination.non-positive-page",
          "specified" -> page.toString))
      }
    }

    raw.offset.foreach { offset =>
      if (offset < 0) {
        return Left(ClientError("pagination.negative-offset",
          "specified" -> offset.toString))
      }
    }

    calculateStyle(raw, config) match {
      case PrecedenceBasedPagination => Right(new PrecedenceBasedPaginationParams(raw, config))
      case _ => Right(new OffsetBasedPaginationParams(raw, config))
    }
  }

}
