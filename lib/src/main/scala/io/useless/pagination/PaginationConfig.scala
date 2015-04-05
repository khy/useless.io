package io.useless.pagination

case class PaginationConfig(
  defaultStyle: PaginationStyle,
  maxLimit: Int,
  defaultLimit: Int,
  defaultOffset: Int,
  validOrders: Seq[String],
  defaultOrder: String
) {

  require(maxLimit > 0, "maxLimit is not greater than zero")

  require(defaultLimit > 0, "defaultLimit is not greater than zero")

  require(defaultLimit <= maxLimit, "defaultLimit is greater than maxLimit")

  require(defaultOffset >= 0, "defaultOffset is negative")

  require(validOrders.contains(defaultOrder), "defaultOrder is not included in validOrders")

}
