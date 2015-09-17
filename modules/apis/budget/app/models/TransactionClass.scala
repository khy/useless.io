package models.budget

import models.budget.util._

sealed class TransactionClass(
  val key: String,
  val name: String
) extends NamedEnum

object TransactionClass extends NamedEnumCompanion[TransactionClass] {
  case object Credit extends TransactionClass("credit", "Credit")
  case object Debit extends TransactionClass("checking", "Checking")
  case class Unknown(override val key: String) extends TransactionClass(key, "Unknown")

  val values = Seq(Credit, Debit)
  def unknown(key: String) = Unknown(key)
}
