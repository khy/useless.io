package models.budget

import java.util.UUID
import org.joda.time.DateTime
import io.useless.account.User

case class TransactionConfirmation(
  guid: UUID,
  createdBy: User,
  createdAt: DateTime
)
