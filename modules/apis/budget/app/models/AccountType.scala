sealed trait AccountType {
  def key: String
}

object AccountType {
  case object Credit extends AccountType { val key = "credit" }
  case object Debit extends AccountType { val key = "debit" }
  case object Savings extends AccountType { val key = "savings" }
  case object External extends AccountType { val key = "external" }
  case class Unknown(key: String) extends AccountType
}
