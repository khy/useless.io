package models.budget.aggregates

import java.util.UUID
import org.joda.time.LocalDate

import models.budget.TransactionType

case class TransactionTypeRollup(
  transactionType: TransactionType,
  fromDate: Option[LocalDate],
  toDate: Option[LocalDate],
  transactionAmountTotal: BigDecimal
)
