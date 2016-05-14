package models.haiku

import java.util.UUID
import org.joda.time.DateTime
import io.useless.account.User

case class ShallowHaiku(
  guid: UUID,
  inResponseToGuid: Option[UUID],
  lines: Seq[String],
  attribution: Option[String],
  createdAt: DateTime,
  createdBy: User
)
