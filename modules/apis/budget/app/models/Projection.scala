package models.budget

import java.util.UUID
import org.joda.time.DateTime
import io.useless.account.User

case class Projection(
  guid: UUID,
  name: String,
  createdBy: User,
  createdAt: DateTime
)
