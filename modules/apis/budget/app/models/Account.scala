package models.budget

import java.util.UUID
import org.joda.time.DateTime
import io.useless.account.User

case class Account(
  guid: UUID,
  accountType: AccountType,
  name: String,
  initialBalance: BigDecimal,
  balance: BigDecimal,
  createdBy: User,
  createdAt: DateTime
)
