package models.budget

import models.budget.util._

sealed trait TransactionType extends Keyed

object TransactionType extends KeyedResolver[TransactionType] {
  case object Credit extends TransactionType { val key = "credit" }
  case object Debit extends TransactionType { val key = "checking" }
  case class Unknown(key: String) extends TransactionType

  val values = Seq(Credit, Debit)
  def unknown(key: String) = Unknown(key)
}
