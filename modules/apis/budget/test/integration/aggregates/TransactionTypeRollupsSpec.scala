package test.budget.integration.aggregates

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import org.joda.time.LocalDate

import models.budget.JsonImplicits._
import models.budget.aggregates.TransactionTypeRollup
import services.budget.TestService
import test.budget.integration.util.IntegrationHelper

class TransactionTypeRollupsSpec
  extends PlaySpec
  with OneServerPerSuite
  with IntegrationHelper
{

  "GET /aggregates/transactionTypeRollups" must {

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/aggregates/transactionTypeRollups").get }
      response.status mustBe UNAUTHORIZED
    }

    "return rollups for all of the requestors transaction types, between the specified dates" in {
      TestService.deleteAccounts()
      TestService.deleteTransactions()
      val account = TestService.createAccount()

      lazy val expense = TestService.getSystemTransactionType("Expense")
      lazy val income = TestService.getSystemTransactionType("Income")

      val salary = TestService.createTransactionType(
        name = "Salary",
        parentGuid = Some(income.guid)
      )

      val car = TestService.createTransactionType(
        name = "Car",
        parentGuid = Some(expense.guid)
      )

      val parkingGarage = TestService.createTransactionType(
        name = "Parking Garage",
        parentGuid = Some(car.guid)
      )

      TestService.createTransaction(
        accountGuid = account.guid,
        transactionTypeGuid = salary.guid,
        date = LocalDate.now.minusDays(5),
        amount = 10000.0
      )

      // Should be excluded since it's on the day before 'fromDate'
      TestService.createTransaction(
        accountGuid = account.guid,
        transactionTypeGuid = parkingGarage.guid,
        date = LocalDate.now.minusDays(11),
        amount = -50.0
      )

      TestService.createTransaction(
        accountGuid = account.guid,
        transactionTypeGuid = parkingGarage.guid,
        date = LocalDate.now.minusDays(8),
        amount = -50.0
      )

      TestService.createTransaction(
        accountGuid = account.guid,
        transactionTypeGuid = parkingGarage.guid,
        date = LocalDate.now.minusDays(3),
        amount = -50.0
      )

      // Should be excluded since it's on the same day as 'toDate'
      TestService.createTransaction(
        accountGuid = account.guid,
        transactionTypeGuid = parkingGarage.guid,
        date = LocalDate.now.minusDays(1),
        amount = -50.0
      )

      TestService.createTransaction(
        accountGuid = account.guid,
        transactionTypeGuid = car.guid,
        date = LocalDate.now.minusDays(5),
        amount = -75.0
      )

      val response = await {
        authenticatedRequest("/aggregates/transactionTypeRollups").
          withQueryString(
            "fromDate" -> LocalDate.now.minusDays(10).toString,
            "toDate" -> LocalDate.now.minusDays(1).toString
          ).get()
      }

      response.status mustBe OK
      val rollups = response.json.as[Seq[TransactionTypeRollup]]

      val salaryRollup = rollups.find(_.transactionType.guid == salary.guid).get
      salaryRollup.transactionAmountTotal mustBe 10000.0

      val parkingGarageRollup = rollups.find(_.transactionType.guid == parkingGarage.guid).get
      parkingGarageRollup.transactionAmountTotal mustBe -100.0

      val carRollup = rollups.find(_.transactionType.guid == car.guid).get
      carRollup.transactionAmountTotal mustBe -75.0
    }

  }

}
