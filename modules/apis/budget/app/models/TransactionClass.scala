package models.budget

import models.budget.util._

sealed class TransactionClass(
  val key: String,
  val name: String
) extends NamedEnum

object TransactionClass extends NamedEnumCompanion[TransactionClass] {
  case object Income extends TransactionClass("income", "Income")
  case object Expense extends TransactionClass("expense", "Expense")
  case class Unknown(override val key: String) extends TransactionClass(key, "Unknown")

  val values = Seq(Income, Expense)
  def unknown(key: String) = Unknown(key)
}
