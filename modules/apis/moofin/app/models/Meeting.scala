package models.moofin

import java.util.UUID
import org.joda.time.DateTime
import io.useless.account.User

case class Meeting(
  guid: UUID,
  date: DateTime,
  createdBy: User,
  createdAt: DateTime
)
