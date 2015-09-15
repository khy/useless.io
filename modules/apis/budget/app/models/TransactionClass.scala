package models.budget

import models.budget.util._

sealed trait TransactionClass extends Keyed

object TransactionClass extends KeyedResolver[TransactionClass] {
  case object Credit extends TransactionClass { val key = "credit" }
  case object Debit extends TransactionClass { val key = "checking" }
  case class Unknown(key: String) extends TransactionClass

  val values = Seq(Credit, Debit)
  def unknown(key: String) = Unknown(key)
}
