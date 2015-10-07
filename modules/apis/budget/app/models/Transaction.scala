package models.budget

import java.util.UUID
import org.joda.time.DateTime
import io.useless.account.User

case class Transaction(
  guid: UUID,
  transactionTypeGuid: UUID,
  accountGuid: UUID,
  amount: BigDecimal,
  timestamp: DateTime,
  confirmation: Option[TransactionConfirmation],
  adjustedTransactionGuid: Option[UUID],
  createdBy: User,
  createdAt: DateTime
)
