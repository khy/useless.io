package test.budget.integration

import java.util.UUID
import org.scalatest._
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.PostgresDriver.api._

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

    "not return any soft-deleted transactions" in {
      TestService.deleteTransactionTypes()
      val expense = TestService.getSystemTransactionType("Expense")
      val income = TestService.getSystemTransactionType("Income")

      val rent = TestService.createTransactionType(
        name = "Rent",
        parentGuid = Some(expense.guid)
      )

      val salary = TestService.createTransactionType(
        name = "Salary",
        parentGuid = Some(income.guid)
      )

      val adjustResponse = await {
        authenticatedRequest(s"/transactionTypes/${rent.guid}/adjustments").
          post(Json.obj("name" -> "RENT"))
      }
      val adjustedRent = adjustResponse.json.as[TransactionType]

      val response = await { authenticatedRequest("/transactionTypes").get }
      val transactionTypeGuids = response.json.as[Seq[TransactionType]].map(_.guid)
      transactionTypeGuids must contain (salary.guid)
      transactionTypeGuids must contain (adjustedRent.guid)
      transactionTypeGuids must not contain (rent.guid)
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
      val subTransactionType = TestService.createTransactionType(
        name = "Birthday Presents", parentGuid = Some(transactionType.guid)
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

      val _subTransactionType = await {
        TestService.transactionTypesService.findTransactionTypes(guids = Some(Seq(subTransactionType.guid)))
      }.toSuccess.value.items.head
      _subTransactionType.parentGuid mustBe Some(_transactionType.guid)

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

    "maintain historical records, when just changing parent" in {
      val tt = TestService.createTransactionType(
        name = "Books", parentGuid = Some(expense.guid)
      )

      val adjustResponse = await {
        authenticatedRequest(s"/transactionTypes/${tt.guid}/adjustments").
          post(Json.obj("parentGuid" -> income.guid))
      }
      val adjustedTransactionType = adjustResponse.json.as[TransactionType]

      val transactionType = getTransactionTypeRecord(tt.guid)
      val transactionTypeSubtypes = getTransactionTypeSubtypesForChild(transactionType.id)
      transactionTypeSubtypes.length mustBe 2

      val oldTransactionTypeSubtype = transactionTypeSubtypes.filter(_.deletedAt.isDefined).head
      val newTransactionTypeSubtype = transactionTypeSubtypes.filter(_.deletedAt.isEmpty).head
      newTransactionTypeSubtype.adjustedTransactionTypeSubtypeId mustBe Some(oldTransactionTypeSubtype.id)
    }

    "maintain historical records, when changing name" in {
      val transactionType = TestService.createTransactionType(
        name = "Books", parentGuid = Some(expense.guid)
      )
      val transaction1 = TestService.createTransaction(transactionTypeGuid = transactionType.guid)
      val transaction2 = TestService.createTransaction(transactionTypeGuid = transactionType.guid)

      val adjustResponse = await {
        authenticatedRequest(s"/transactionTypes/${transactionType.guid}/adjustments").
          post(Json.obj("name" -> "Wedding Presents", "parentGuid" -> income.guid))
      }
      val adjustedTransactionType = adjustResponse.json.as[TransactionType]

      val oldTransactionType = getTransactionTypeRecord(transactionType.guid)
      val newTransactionType = getTransactionTypeRecord(adjustedTransactionType.guid)

      oldTransactionType.deletedAt mustBe 'defined
      newTransactionType.adjustedTransactionTypeId mustBe Some(oldTransactionType.id)

      val oldTttQuery = TransactionTransactionTypes.filter(_.transactionTypeId === oldTransactionType.id)
      val oldTransactionTransactionTypes = await { database.run(oldTttQuery.result) }
      oldTransactionTransactionTypes.length mustBe 2
      oldTransactionTransactionTypes.foreach { transactionTransactionType =>
        transactionTransactionType.deletedAt mustBe 'defined
      }

      val newTttQuery = TransactionTransactionTypes.filter(_.transactionTypeId === newTransactionType.id)
      val newTransactionTransactionTypes = await { database.run(newTttQuery.result) }
      newTransactionTransactionTypes.map(_.adjustedTransactionTransactionTypeId) mustBe
        oldTransactionTransactionTypes.map { ttt => Some(ttt.id) }

      val oldTransactionTypeSubtype = getTransactionTypeSubtypesForChild(oldTransactionType.id).head
      val newTransactionTypeSubtype = getTransactionTypeSubtypesForChild(newTransactionType.id).head

      // We intentionally do not delete the old TransactionType's
      // TransactionTypeSubtype - see the TransactionTypeService
      oldTransactionTypeSubtype.deletedAt mustBe 'empty
      newTransactionTypeSubtype.adjustedTransactionTypeSubtypeId mustBe Some(oldTransactionTypeSubtype.id)
    }

    def assertTransactionsType(transactionGuids: Seq[UUID], typeGuid: UUID) {
      val records = await { TestService.transactionsService.findTransactions(
        guids = Some(transactionGuids)
      )}.toSuccess.value.items
      val transactions = await { TestService.transactionsService.records2models(records) }
      transactions.map(_.transactionTypeGuid).distinct mustBe Seq(typeGuid)
    }

    def getTransactionTypeRecord(guid: UUID) = await {
      database.run(TransactionTypes.filter(_.guid === guid).result)
    }.head

    def getTransactionTypeSubtypesForChild(id: Long) = {
      val query = TransactionTypeSubtypes.filter(_.childTransactionTypeId === id)
      await { database.run(query.result) }
    }

  }

}
