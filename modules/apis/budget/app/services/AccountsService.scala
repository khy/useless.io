package services.budget

import scala.concurrent.Future
import org.joda.time.DateTime
import io.useless.validation._

import models.budget.{Account, AccountType}
import models.budget.JsonImplicits._

class AccountsService {

  def createAccount(
    accountType: AccountType,
    name: String
  ): Future[Validation[Account]] = {
    Future.successful(Validation.failure("jah", "der"))
  }

}
