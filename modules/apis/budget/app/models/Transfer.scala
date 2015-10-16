package models.budget

import java.util.UUID
import org.joda.time.DateTime
import io.useless.account.User

case class Transfer(
  guid: UUID,
  fromTransaction: Transaction,
  toTransaction: Transaction,
  createdBy: User,
  createdAt: DateTime
)
