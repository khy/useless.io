package models.budget

import java.util.UUID
import org.joda.time.DateTime
import io.useless.account.User

case class Context(
  guid: UUID,
  name: String,
  users: Seq[User],
  createdBy: User,
  createdAt: DateTime,
  deletedBy: Option[User],
  deletedAt: Option[DateTime]
)
