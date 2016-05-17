package models.haiku

import java.util.UUID
import org.joda.time.DateTime
import io.useless.account.User

case class ShallowHaiku(
  guid: UUID,
  lines: Seq[String],
  attribution: Option[String],
  inResponseToGuid: Option[UUID],
  responseGuids: Seq[UUID],
  createdAt: DateTime,
  createdBy: User
)
