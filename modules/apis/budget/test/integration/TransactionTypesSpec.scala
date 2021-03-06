package test.budget.integration

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.PostgresDriver.api._
import io.useless.validation.Errors
import io.useless.play.json.validation.ErrorsJson._
import io.useless.validation.ValidationTestHelper._

import models.budget.{TransactionType, TransactionTypeOwnership}
import models.budget.JsonImplicits._
import services.budget.TestService
import db.budget._
import db.budget.util.DatabaseAccessor
import test.budget.integration.util.IntegrationHelper

class TransactionTypesSpec
  extends PlaySpec
  with OneServerPerSuite
  with IntegrationHelper
  with DatabaseAccessor
{

  "GET /transactionTypes" must {

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/transactionTypes").get }
      response.status mustBe UNAUTHORIZED
    }

    lazy val expense = TestService.getSystemTransactionType("Expense")
    lazy val income = TestService.getSystemTransactionType("Income")

    "return 200 OK with transaction types for accounts in the user's contexts, along with system transaction types" in {
      TestService.deleteTransactionTypes()

      TestService.createTransactionType(
        contextGuid = TestService.otherContext.guid,
        name = "Rent",
        parentGuid = Some(expense.guid),
        ownership = TransactionTypeOwnership.User,
        accessToken = TestService.otherAccessToken
      )

      TestService.createTransactionType(
        name = "Salary",
        parentGuid = Some(income.guid),
        ownership = TransactionTypeOwnership.System,
        accessToken = TestService.otherAccessToken
      )

      TestService.createTransactionType(
        contextGuid = TestService.sharedContext.guid,
        name = "Kevin's Salary",
        parentGuid = Some(income.guid),
        ownership = TransactionTypeOwnership.User,
        accessToken = TestService.otherAccessToken
      )

      TestService.createTransactionType(
        contextGuid = TestService.myContext.guid,
        name = "Dominos",
        parentGuid = Some(expense.guid),
        ownership = TransactionTypeOwnership.User,
        accessToken = TestService.accessToken
      )

      val response = await { authenticatedRequest("/transactionTypes").get }
      response.status mustBe OK
      val transactionTypeNames = response.json.as[Seq[TransactionType]].map(_.name)
      transactionTypeNames must contain ("Salary")
      transactionTypeNames must contain ("Kevin's Salary")
      transactionTypeNames must contain ("Dominos")
      transactionTypeNames must not contain ("Rent")
    }

    "return transaction types limited to the specified context" in {
      TestService.deleteTransactionTypes()

      TestService.createTransactionType(
        contextGuid = TestService.sharedContext.guid,
        name = "Rent",
        parentGuid = Some(expense.guid),
        ownership = TransactionTypeOwnership.User,
        accessToken = TestService.otherAccessToken
      )

      TestService.createTransactionType(
        contextGuid = TestService.myContext.guid,
        name = "Groceries",
        parentGuid = Some(expense.guid),
        ownership = TransactionTypeOwnership.User,
        accessToken = TestService.accessToken
      )

      val response = await {
        authenticatedRequest("/transactionTypes").withQueryString(
          "context" -> TestService.sharedContext.guid.toString
        ).get
      }
      val transactionTypeNames = response.json.as[Seq[TransactionType]].map(_.name)
      transactionTypeNames must contain ("Rent")
      transactionTypeNames must not contain ("Groceries")
    }

  }

  "POST /transactionTypes" must {

    lazy val expense = TestService.getSystemTransactionType("Expense")
    lazy val context = TestService.createContext()

    lazy val json = Json.obj(
      "contextGuid" -> context.guid,
      "name" -> "Rent",
      "parentGuid" -> expense.guid
    )

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/transactionTypes").post(json) }
      response.status mustBe UNAUTHORIZED
    }

    "return a 409 Conflict any required fields aren't specified" in {
      Seq("name").foreach { field =>
        val response = await { authenticatedRequest("/transactionTypes").post(json - field) }
        response.status mustBe CONFLICT
      }
    }

    "return a 409 Conflict if name is an empty string" in {
      val _json = json ++ Json.obj("name" -> "")
      val response = await { authenticatedRequest("/transactionTypes").post(_json) }
      response.status mustBe CONFLICT
    }

    "return a 409 if the name already exists in the context" in {
      TestService.createTransactionType(
        contextGuid = context.guid,
        name = "Rent",
        parentGuid = Some(expense.guid)
      )

      val response1 = await { authenticatedRequest("/transactionTypes").post(json) }
      response1.status mustBe CONFLICT

      val otherContext = TestService.createContext()
      val _json = json ++ Json.obj("contextGuid" -> otherContext.guid)
      val response2 = await { authenticatedRequest("/transactionTypes").post(_json) }
      response2.status mustBe CREATED
    }

    "return a new TransactionType if authorized and valid" in {
      TestService.deleteTransactionTypes()
      val response = await { authenticatedRequest("/transactionTypes").post(json) }
      response.status mustBe CREATED

      val transactionType = response.json.as[TransactionType]
      transactionType.contextGuid mustBe Some(context.guid)
      transactionType.name mustBe "Rent"
      transactionType.parentGuid mustBe Some(expense.guid)
    }

  }

  "POST /transactionTypes/:guid/adjustments" must {

    lazy val expense = TestService.getSystemTransactionType("Expense")
    lazy val income = TestService.getSystemTransactionType("Income")

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val transactionType = TestService.createTransactionType(
        name = "Presents", parentGuid = Some(expense.guid)
      )

      val response = await {
        unauthentictedRequest(s"/transactionTypes/${transactionType.guid}/adjustments").
          post(Json.obj("name" -> "Wedding Presents"))
      }
      response.status mustBe UNAUTHORIZED
    }

    "return a 409 Conflict if the specified TransactionType is a 'system' TransactionType" in {
      val response = await {
        authenticatedRequest(s"/transactionTypes/${expense.guid}/adjustments").
          post(Json.obj("name" -> "Expense!!!!"))
      }
      response.status mustBe CONFLICT
      val errors = response.json.as[Seq[Errors]]
      errors.getMessages("guid").head.key mustBe "useless.error.systemTransactionType"

      val _expense = TestService.getSystemTransactionType("Expense")
      expense mustBe _expense
    }

    "return the same TransactionType with a new parent and new name" in {
      val transactionType = TestService.createTransactionType(
        name = "Presents", parentGuid = Some(expense.guid)
      )
      val transaction = TestService.createTransaction(transactionTypeGuid = transactionType.guid)

      val response = await {
        authenticatedRequest(s"/transactionTypes/${transactionType.guid}/adjustments").
          post(Json.obj("parentGuid" -> income.guid, "name" -> "Wedding Presents"))
      }
      response.status mustBe CREATED
      val _transactionType = response.json.as[TransactionType]
      _transactionType.guid mustBe transactionType.guid
      _transactionType.name mustBe "Wedding Presents"
      _transactionType.parentGuid mustBe Some(income.guid)

      val transactionTypeRecord = await { database.run(TransactionTypes.filter(_.guid === transactionType.guid).result) }.head
      val ttsQuery = TransactionTypeSubtypes.filter(_.childTransactionTypeId === transactionTypeRecord.id)
      val transactionTypeSubtypes = await { database.run(ttsQuery.result) }
      transactionTypeSubtypes.length mustBe 2
      transactionTypeSubtypes.filter(_.deletedAt.isDefined).headOption mustBe 'defined
      transactionTypeSubtypes.filter(_.deletedAt.isEmpty).headOption mustBe 'defined

      val records = await { TestService.transactionsService.findTransactions(
        guids = Some(Seq(transaction.guid))
      )}.toSuccess.value.items
      val transactions = await { TestService.transactionsService.records2models(records) }
      transactions.map(_.transactionTypeGuid).distinct mustBe Seq(transactionType.guid)
    }

  }

}
