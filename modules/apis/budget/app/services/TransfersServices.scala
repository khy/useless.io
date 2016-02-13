package services.budget

import java.util.{Date, UUID}
import java.sql.Timestamp
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
import org.joda.time.{LocalDate, DateTime}
import io.useless.accesstoken.AccessToken
import io.useless.pagination._
import io.useless.validation._

import models.budget._
import db.budget._
import db.budget.util.DatabaseAccessor
import services.budget.util._

object TransfersService {

  def default()(implicit app: Application) = new TransfersService(
    TransactionsService.default(),
    UsersHelper.default()
  )

}

class TransfersService(
  transactionService: TransactionsService,
  usersHelper: UsersHelper
) extends DatabaseAccessor {

  def records2models(
    records: Seq[TransferRecord]
  )(implicit ec: ExecutionContext): Future[Seq[Transfer]] = {
    val userGuids = records.map(_.createdByAccount)
    val futUsers = usersHelper.getUsers(userGuids)

    val transactionIds = records.map(_.fromTransactionId) ++ records.map(_.toTransactionId)
    val futTransactionMap = transactionService.findTransactions(ids = Some(transactionIds)).flatMap { result =>
      val transactionRecords = result.toSuccess.value.items
      transactionService.records2models(transactionRecords).map { transactions =>
        transactionRecords.map { transactionRecord =>
          val transaction = transactions.find { _.guid == transactionRecord.guid }.get
          (transactionRecord.id, transaction)
        }.toMap
      }
    }

    for {
      users <- futUsers
      transactionMap <- futTransactionMap
    } yield {
      records.map { record =>
        Transfer(
          guid = record.guid,
          fromTransaction = transactionMap.get(record.fromTransactionId).getOrElse {
            throw new ResourceUnexpectedlyNotFound("Transaction", record.fromTransactionId)
          },
          toTransaction = transactionMap.get(record.toTransactionId).getOrElse {
            throw new ResourceUnexpectedlyNotFound("Transaction", record.toTransactionId)
          },
          createdBy = users.find(_.guid == record.createdByAccount).getOrElse(UsersHelper.AnonUser),
          createdAt = new DateTime(record.createdAt)
        )
      }
    }
  }

  def findTransfers(
    ids: Option[Seq[Long]],
    rawPaginationParams: RawPaginationParams = RawPaginationParams()
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[TransferRecord]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams)

    ValidationUtil.future(valPaginationParams) { paginationParams =>
      var query = Transfers.filter(_.deletedAt.isEmpty)

      ids.foreach { ids =>
        query = query.filter { _.id inSet ids }
      }

      database.run(query.result).map { transferRecords =>
        PaginatedResult.build(transferRecords, paginationParams, None)
      }
    }
  }

  def createTransfer(
    fromAccountGuid: UUID,
    toAccountGuid: UUID,
    amount: BigDecimal,
    date: LocalDate,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[TransferRecord]] = {
    val transactionTypeQuery = TransactionTypes.filter(_.name === "Transfer").map(_.guid)
    val futTransferGuid = database.run(transactionTypeQuery.result).map { results =>
      results.headOption.getOrElse {
        throw new ResourceUnexpectedlyNotFound("TransactionType", "Transfer")
      }
    }

    val futFromContextIds = database.run {
      Accounts.filter { _.guid === fromAccountGuid }.map { _.contextId }.result
    }

    val futToContextIds = database.run {
      Accounts.filter { _.guid === toAccountGuid }.map { _.contextId }.result
    }

    val futValToAccount = for {
      fromContextIds <- futFromContextIds
      toContextIds <- futToContextIds
    } yield {
      fromContextIds.headOption.flatMap { fromContextId =>
        toContextIds.headOption.map { toContextId =>
          if (fromContextId != toContextId) {
            Validation.failure(
              "toAccountGuid",
              "useless.error.budget.invalidTransferToAccount",
              "specified" -> toAccountGuid.toString
            )
          } else {
            Validation.success(Unit)
          }
        }
      }.getOrElse { Validation.success(Unit) }
    }

    futValToAccount.flatMap { valToAccount =>
      ValidationUtil.flatMapFuture(valToAccount) { _ =>
        for {
          transferGuid <- futTransferGuid
          valFromTransaction <- transactionService.createTransaction(
            transactionTypeGuid = transferGuid,
            accountGuid = fromAccountGuid,
            amount = -amount,
            date = date,
            name = None,
            plannedTransactionGuid = None,
            adjustedTransactionGuid = None,
            accessToken = accessToken
          )
          valToTransaction <- transactionService.createTransaction(
            transactionTypeGuid = transferGuid,
            accountGuid = toAccountGuid,
            amount = amount,
            date = date,
            name = None,
            plannedTransactionGuid = None,
            adjustedTransactionGuid = None,
            accessToken = accessToken
          )
          valTransfer <- ValidationUtil.future(valFromTransaction ++ valToTransaction) {
            case (fromTransaction, toTransaction) =>

            val transfers = Transfers.map { r =>
              (r.guid, r.fromTransactionId, r.toTransactionId, r.createdByAccount, r.createdByAccessToken)
            }.returning(Transfers.map(_.id))

            val insert = transfers += ((
              UUID.randomUUID,
              fromTransaction.id,
              toTransaction.id,
              accessToken.resourceOwner.guid,
              accessToken.guid
            ))

            database.run(insert).flatMap { id =>
              findTransfers(ids = Some(Seq(id))).map { result =>
                result.map(_.items.headOption) match {
                  case Validation.Success(Some(transfer)) => transfer
                  case _ => throw new ResourceUnexpectedlyNotFound("Transfer", id)
                }
              }
            }
          }
        } yield valTransfer
      }
    }
  }

}
