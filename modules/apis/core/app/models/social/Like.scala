package models.core.social

import java.util.UUID
import org.joda.time.DateTime
import io.useless.account.User

case class Like(
  guid: UUID,
  resourceApi: String,
  resourceType: String,
  resourceId: String,
  createdAt: DateTime,
  createdBy: User
)
