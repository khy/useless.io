package models.budget.aggregates

import java.util.UUID

case class MonthRollup(
  guid: UUID,
  year: Int,
  month: Int
)
