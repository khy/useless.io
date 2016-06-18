package models.haiku

import java.util.UUID
import org.joda.time.DateTime
import io.useless.account.User

case class Haiku(
  guid: UUID,
  lines: Seq[String],
  attribution: Option[String],
  inResponseTo: Option[ShallowHaiku],
  responseCount: Int,
  createdAt: DateTime,
  createdBy: User
)
