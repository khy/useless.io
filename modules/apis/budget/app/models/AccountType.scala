package models.budget

import io.useless.{NamedEnum, NamedEnumCompanion}

sealed class AccountType(
  val key: String,
  val name: String
) extends NamedEnum

object AccountType extends NamedEnumCompanion[AccountType] {
  case object Credit extends AccountType("credit", "Credit")
  case object Checking extends AccountType("checking", "Checking")
  case object Savings extends AccountType("savings", "Savings")
  case class Unknown(override val key: String) extends AccountType(key, "Unknown")

  val values = Seq(Credit, Checking, Savings)
  def unknown(key: String) = Unknown(key)
}
