package models.budget

import java.util.UUID
import org.joda.time.DateTime
import io.useless.account.User

case class PlannedTransaction(
  guid: UUID,
  transactionTypeGuid: UUID,
  accountGuid: UUID,
  minAmount: Option[BigDecimal],
  maxAmount: Option[BigDecimal],
  minTimestamp: Option[DateTime],
  maxTimestamp: Option[DateTime],
  createdBy: User,
  createdAt: DateTime
)
