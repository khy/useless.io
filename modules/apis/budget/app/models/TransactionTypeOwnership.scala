package models.budget

import models.budget.util._

sealed class TransactionTypeOwnership(
  val key: String,
  val name: String
) extends NamedEnum

object TransactionTypeOwnership extends NamedEnumCompanion[TransactionTypeOwnership] {
  case object System extends TransactionTypeOwnership("system", "System")
  case object User extends TransactionTypeOwnership("user", "User")
  case class Unknown(override val key: String) extends TransactionTypeOwnership(key, "Unknown")

  val values = Seq(System, User)
  def unknown(key: String) = Unknown(key)
}
