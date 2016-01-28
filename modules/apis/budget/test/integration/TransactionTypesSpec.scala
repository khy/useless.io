package test.budget.integration

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import models.budget.{TransactionType, TransactionTypeOwnership}
import models.budget.JsonImplicits._
import services.budget.TestService
import test.budget.integration.util.IntegrationHelper

class TransactionTypesSpec
  extends PlaySpec
  with OneServerPerSuite
  with IntegrationHelper
{

  "GET /transactionTypes" must {

    "return a 401 Unauthorized if the request isn't authenticated" in {
      val response = await { unauthentictedRequest("/transactionTypes").get }
      response.status mustBe UNAUTHORIZED
    }

    "return 200 OK with transaction types that the requestor owns, along with system transaction types" in {
      TestService.deleteTransactionTypes()
      val expense = TestService.getSystemTransactionType("Expense")
      val income = TestService.getSystemTransactionType("Income")

      TestService.createTransactionType(
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
        name = "Dominos",
        parentGuid = Some(expense.guid),
        ownership = TransactionTypeOwnership.User,
        accessToken = TestService.accessToken
      )

      val response = await { authenticatedRequest("/transactionTypes").get }
      response.status mustBe OK
      val transactionTypeNames = response.json.as[Seq[TransactionType]].map(_.name)
      transactionTypeNames must contain ("Salary")
      transactionTypeNames must contain ("Dominos")
      transactionTypeNames must not contain ("Rent")
    }

  }

  "POST /transactionTypes" must {

    lazy val expense = TestService.getSystemTransactionType("Expense")

    lazy val json = Json.obj(
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

    "return a new TransactionType if authorized and valid" in {
      val response = await { authenticatedRequest("/transactionTypes").post(json) }
      response.status mustBe CREATED

      val transactionType = response.json.as[TransactionType]
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
      ((response.json \ "guid")(0) \ "key").as[String] mustBe "useless.error.systemTransactionType"

      val _expense = TestService.getSystemTransactionType("Expense")
      expense mustBe _expense
    }

    "return the same TransactionType with a new parent, if just changing the parent" in {
      val transactionType = TestService.createTransactionType(
        name = "Presents", parentGuid = Some(expense.guid)
      )
      val transaction = TestService.createTransaction(transactionTypeGuid = transactionType.guid)

      val response = await {
        authenticatedRequest(s"/transactionTypes/${transactionType.guid}/adjustments").
          post(Json.obj("parentGuid" -> income.guid))
      }
      response.status mustBe CREATED
      val _transactionType = response.json.as[TransactionType]
      _transactionType.guid mustBe transactionType.guid
      _transactionType.name mustBe "Presents"
      _transactionType.parentGuid mustBe Some(income.guid)

      assertTransactionsType(
        transactionGuids = Seq(transaction.guid),
        typeGuid = transactionType.guid
      )
    }

    "return a new TransactionType with the same relationships, if changing the name" in {
      val transactionType = TestService.createTransactionType(
        name = "Presents", parentGuid = Some(expense.guid)
      )
      val transaction1 = TestService.createTransaction(transactionTypeGuid = transactionType.guid)
      val transaction2 = TestService.createTransaction(transactionTypeGuid = transactionType.guid)

      val response = await {
        authenticatedRequest(s"/transactionTypes/${transactionType.guid}/adjustments").
          post(Json.obj("name" -> "Wedding Presents"))
      }
      response.status mustBe CREATED
      val _transactionType = response.json.as[TransactionType]
      _transactionType.guid must not be transactionType.guid
      _transactionType.name mustBe "Wedding Presents"
      _transactionType.parentGuid mustBe Some(expense.guid)

      assertTransactionsType(
        transactionGuids = Seq(transaction1.guid, transaction2.guid),
        typeGuid = _transactionType.guid
      )
    }

    "return a new TransactionType with a new parent, but same Transactions, if changing both name and parent" in {
      val transactionType = TestService.createTransactionType(
        name = "Presents", parentGuid = Some(expense.guid)
      )
      val transaction1 = TestService.createTransaction(transactionTypeGuid = transactionType.guid)
      val transaction2 = TestService.createTransaction(transactionTypeGuid = transactionType.guid)

      val response = await {
        authenticatedRequest(s"/transactionTypes/${transactionType.guid}/adjustments").
          post(Json.obj("name" -> "Wedding Presents", "parentGuid" -> income.guid))
      }
      response.status mustBe CREATED
      val _transactionType = response.json.as[TransactionType]
      _transactionType.guid must not be transactionType.guid
      _transactionType.name mustBe "Wedding Presents"
      _transactionType.parentGuid mustBe Some(income.guid)

      assertTransactionsType(
        transactionGuids = Seq(transaction1.guid, transaction2.guid),
        typeGuid = _transactionType.guid
      )
    }

    def assertTransactionsType(transactionGuids: Seq[UUID], typeGuid: UUID) {
      val records = await { TestService.transactionsService.findTransactions(
        guids = Some(transactionGuids)
      )}.toSuccess.value.items
      val transactions = await { TestService.transactionsService.records2models(records) }
      transactions.map(_.transactionTypeGuid).distinct mustBe Seq(typeGuid)
    }

  }

}
