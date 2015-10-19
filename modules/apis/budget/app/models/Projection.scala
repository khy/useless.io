package models.budget

import java.util.UUID
import org.joda.time.LocalDate

case class Projection(
  account: Account,
  date: LocalDate,
  minAmount: BigDecimal,
  maxAmount: BigDecimal
)
