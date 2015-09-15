package models.budget

import java.util.UUID
import org.joda.time.DateTime
import io.useless.account.User

case class TransactionType(
  guid: UUID,
  accountGuid: UUID,
  transactionClass: TransactionClass,
  name: String,
  createdBy: User,
  createdAt: DateTime
)
