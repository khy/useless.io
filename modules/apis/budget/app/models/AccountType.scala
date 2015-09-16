package models.budget

import models.budget.util._

sealed trait AccountType extends Keyed

object AccountType extends KeyedResolver[AccountType] {
  case object Credit extends AccountType { val key = "credit" }
  case object Checking extends AccountType { val key = "checking" }
  case object Savings extends AccountType { val key = "savings" }
  case class Unknown(key: String) extends AccountType

  val values = Seq(Credit, Checking, Savings)
  def unknown(key: String) = Unknown(key)
}
