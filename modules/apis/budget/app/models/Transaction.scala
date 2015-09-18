package models.budget

import java.util.UUID
import org.joda.time.DateTime
import io.useless.account.User

case class Transaction(
  guid: UUID,
  transactionTypeGuid: UUID,
  amount: BigDecimal,
  timestamp: DateTime,
  createdBy: User,
  createdAt: DateTime
)
