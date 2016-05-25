package io.useless.pagination

import io.useless.Message
import io.useless.validation.Validation

case class PaginationConfig[T](
  defaultStyle: PaginationStyle,
  maxLimit: Int,
  defaultLimit: Int,
  defaultOffset: Int,
  validOrders: Seq[String],
  defaultOrder: String,
  afterParser: (String) => Seq[Message]
) {

  require(maxLimit > 0, "maxLimit is not greater than zero")

  require(defaultLimit > 0, "defaultLimit is not greater than zero")

  require(defaultLimit <= maxLimit, "defaultLimit is greater than maxLimit")

  require(defaultOffset >= 0, "defaultOffset is negative")

  require(validOrders.contains(defaultOrder), "defaultOrder is not included in validOrders")

}
