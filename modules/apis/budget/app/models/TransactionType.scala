package models.budget

import java.util.UUID
import org.joda.time.DateTime
import io.useless.account.User

case class TransactionType(
  guid: UUID,
  contextGuid: Option[UUID],
  name: String,
  parentGuid: Option[UUID],
  ownership: TransactionTypeOwnership,
  createdBy: User,
  createdAt: DateTime
)
