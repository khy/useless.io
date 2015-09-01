package models.budget

import java.util.UUID
import org.joda.time.DateTime
import io.useless.account.User

case class TransactionGroup(
  guid: UUID,
  accountGuid: UUID,
  transactionType: TransactionType,
  name: String,
  createdBy: User,
  createdAt: DateTime
)
