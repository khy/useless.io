package models.budget

import java.util.UUID
import org.joda.time.{LocalDate, DateTime}
import io.useless.account.User

case class PlannedTransaction(
  guid: UUID,
  transactionTypeGuid: UUID,
  accountGuid: UUID,
  minAmount: Option[BigDecimal],
  maxAmount: Option[BigDecimal],
  minDate: Option[LocalDate],
  maxDate: Option[LocalDate],
  name: Option[String],
  transactionGuid: Option[UUID],
  createdBy: User,
  createdAt: DateTime
)
