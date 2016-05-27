package io.useless.pagination

import java.util.UUID

import io.useless.validation.{Validation, Validator}

/*
 * Minimally-parsed pagination specifications.
 */
case class RawPaginationParams(
  style: Option[PaginationStyle] = None,
  limit: Option[Int] = None,
  order: Option[String] = None,
  offset: Option[Int] = None,
  page: Option[Int] = None,
  after: Option[String] = None
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
    defaultOrder = "created_at",
    afterParser = Validator.uuid(_: String, None).map(_.toString)
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
  ): Validation[PaginationParams] = {
    val limitVal = raw.limit.map { limit =>
      if (limit <= 0) {
        Validation.failure("pagination.limit", "useless.error.non-positive",
          "specified" -> limit.toString)
      } else if (limit > config.maxLimit) {
        Validation.failure("pagination.limit", "useless.error.exceeds-maximum",
          "specified" -> limit.toString, "maximum" -> config.maxLimit.toString)
      } else {
        Validation.success(Some(limit))
      }
    }.getOrElse(Validation.success(None))

    val orderVal = raw.order.map { order =>
      if (!config.validOrders.contains(order)) {
        val formattedValidOptions = config.validOrders.
          sortWith(_.toLowerCase < _.toLowerCase).
          map("'" + _ + "'").mkString(", ")

        Validation.failure("pagination.order", "useless.error.invalid-value",
         "specified" -> order, "valid" -> formattedValidOptions)
      } else {
        Validation.success(Some(order))
      }
    }.getOrElse(Validation.success(None))

    val pageVal = raw.page.map { page =>
      if (page <= 0) {
        Validation.failure("pagination.page", "useless.error.non-positive",
          "specified" -> page.toString)
      } else {
        Validation.success(Some(page))
      }
    }.getOrElse(Validation.success(None))

    val offsetVal = raw.offset.map { offset =>
      if (offset < 0) {
        Validation.failure("pagination.offset", "useless.error.negative",
          "specified" -> offset.toString)
      } else {
        Validation.success(Some(offset))
      }
    }.getOrElse(Validation.success(None))

    val afterVal = raw.after.map { after =>
      config.afterParser(after).fold (
        errors => {
          val scopedErrors = errors.map { _.copy(key = Some("pagination.after")) }
          Validation.Failure(scopedErrors)
        },
        after => Validation.Success(after)
      )
    }.getOrElse(Validation.Success(None))

    (limitVal ++ orderVal ++ pageVal ++ offsetVal ++ afterVal).map { _ =>
      calculateStyle(raw, config) match {
        case PrecedenceBasedPagination => new PrecedenceBasedPaginationParams(raw, config)
        case _ => new OffsetBasedPaginationParams(raw, config)
      }
    }
  }

}
