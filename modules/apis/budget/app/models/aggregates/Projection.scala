package models.budget.aggregates

import java.util.UUID
import org.joda.time.LocalDate

import models.budget.Account

case class Projection(
  account: Account,
  date: LocalDate,
  minBalance: BigDecimal,
  maxBalance: BigDecimal
)
