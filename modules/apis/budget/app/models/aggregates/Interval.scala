package models.budget.aggregates

import java.util.UUID
import org.joda.time.LocalDate

trait Interval {
  def start: LocalDate
  def end: LocalDate
}

trait HistoryInterval extends Interval {
  def transactionAmountTotal: BigDecimal
  def transactionCount: Int
}

trait ProjectionInterval extends Interval {
  def initialBalanceMin: BigDecimal
  def initialBalanceMax: BigDecimal
  def plannedTransactionAmountTotalMin: BigDecimal
  def plannedTransactionAmountTotalMax: BigDecimal
  def plannedTransactionCountMin: Int
  def plannedTransactionCountMax: Int
}

case class AccountHistoryInterval(
  guid: UUID,
  accountGuid: UUID,
  start: LocalDate,
  end: LocalDate,
  initialBalance: BigDecimal,
  transactionAmountTotal: BigDecimal,
  transactionCount: Int
) extends HistoryInterval

case class AccountProjectionInterval(
  guid: UUID,
  accountGuid: UUID,
  start: LocalDate,
  end: LocalDate,
  initialBalanceMin: BigDecimal,
  initialBalanceMax: BigDecimal,
  plannedTransactionAmountTotalMin: BigDecimal,
  plannedTransactionAmountTotalMax: BigDecimal,
  plannedTransactionCountMin: Int,
  plannedTransactionCountMax: Int
) extends ProjectionInterval
