package models.budget

import java.util.UUID
import org.joda.time.DateTime
import io.useless.account.User

case class Transaction(
  guid: UUID,
  transactionGroupGuid: UUID,
  amount: BigDecimal,
  timestamp: DateTime,
  projectionGuid: Option[UUID],
  createdBy: User,
  createdAt: DateTime
)