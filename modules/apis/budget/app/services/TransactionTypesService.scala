package services.budget

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
import org.joda.time.DateTime
import io.useless.accesstoken.AccessToken
import io.useless.pagination._
import io.useless.validation._

import models.budget.{TransactionType, TransactionTypeOwnership}
import db.budget._
import db.budget.util.DatabaseAccessor
import db.budget.util.SqlUtil
import services.budget.util.{UsersHelper, ResourceUnexpectedlyNotFound}

object TransactionTypesService {

  def default()(implicit app: Application) = new TransactionTypesService(UsersHelper.default())

}

class TransactionTypesService(
  usersHelper: UsersHelper
) extends DatabaseAccessor {

  def records2models(records: Seq[TransactionTypeRecord])(implicit ec: ExecutionContext): Future[Seq[TransactionType]] = {
    val userGuids = records.map(_.createdByAccount)
    val futUsers = usersHelper.getUsers(userGuids)

    val parentQuery = TransactionTypeSubtypes.join(TransactionTypes).on { case (tts, tt) =>
      tts.parentTransactionTypeId === tt.id
    }.filter { case (tts, _) =>
      tts.deletedAt.isEmpty &&
      (tts.childTransactionTypeId inSet records.map(_.id))
    }.map { case (tts, tt) =>
      (tts.childTransactionTypeId, tt.guid)
    }
    val futParents = database.run(parentQuery.result)

    for {
      users <- futUsers
      parents <- futParents
    } yield {
      records.map { record =>
        TransactionType(
          guid = record.guid,
          name = record.name,
          parentGuid = parents.find { case (childTransactionTypeId, _) =>
            childTransactionTypeId == record.id
          }.map { case (_, parentGuid) => parentGuid },
          ownership = TransactionTypeOwnership(record.ownershipKey),
          createdBy = users.find(_.guid == record.createdByAccount).getOrElse(UsersHelper.AnonUser),
          createdAt = new DateTime(record.createdAt)
        )
      }
    }
  }

  def findTransactionTypes(
    ids: Option[Seq[Long]] = None,
    names: Option[Seq[String]] = None,
    ownerships: Option[Seq[TransactionTypeOwnership]] = None,
    createdByAccounts: Option[Seq[UUID]] = None,
    rawPaginationParams: RawPaginationParams = RawPaginationParams()
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[TransactionType]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams)

    ValidationUtil.future(valPaginationParams) { paginationParams =>
      var query = TransactionTypes.filter { r => r.deletedAt.isEmpty }

      ids.foreach { ids =>
        query = query.filter { _.id inSet ids }
      }

      names.foreach { names =>
        query = query.filter { _.name inSet names }
      }

      ownerships.foreach { ownerships =>
        query = query.filter { _.ownershipKey inSet ownerships.map(_.key)}
      }

      createdByAccounts.foreach { createdByAccounts =>
        query = query.filter { transactionType =>
          transactionType.createdByAccount.inSet(createdByAccounts) ||
          transactionType.ownershipKey === TransactionTypeOwnership.System.key
        }
      }

      database.run(query.result).flatMap { records =>
        records2models(records).map { transactionTypes =>
          PaginatedResult.build(transactionTypes, paginationParams, None)
        }
      }
    }
  }

  def createTransactionType(
    name: String,
    parentGuid: Option[UUID],
    ownership: TransactionTypeOwnership,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[TransactionType]] = {
    val futValOptParentId = parentGuid.map { parentGuid =>
      val transactionTypeQuery = TransactionTypes.filter { _.guid === parentGuid }
      database.run(transactionTypeQuery.result).map { transactionTypes =>
        transactionTypes.headOption.map { transactionType =>
          Validation.success(Some(transactionType.id))
        }.getOrElse {
          Validation.failure("parentGuid", "useless.error.unknownGuid", "specified" -> parentGuid.toString)
        }
      }
    }.getOrElse {
      Future.successful(Validation.success(None))
    }

    futValOptParentId.flatMap { valOptParentId =>
      ValidationUtil.future(valOptParentId) { case (optParentId) =>
        val transactionTypes = TransactionTypes.map { r =>
          (r.guid, r.name, r.ownershipKey, r.createdByAccount, r.createdByAccessToken)
        }.returning(TransactionTypes.map(_.id))

        val insert = transactionTypes += ((UUID.randomUUID, name, ownership.key, accessToken.resourceOwner.guid, accessToken.guid))

        database.run(insert).flatMap { id =>
          val futParentInsert = optParentId.map { parentId =>
            val transactionTypeSubtypes = TransactionTypeSubtypes.map { r =>
              (r.parentTransactionTypeId, r.childTransactionTypeId, r.createdByAccount, r.createdByAccessToken)
            }.returning(TransactionTypeSubtypes.map(_.id))

            val insert = transactionTypeSubtypes += ((parentId, id, accessToken.resourceOwner.guid, accessToken.guid))

            database.run(insert).map { _ => () }
          }.getOrElse {
            Future.successful(())
          }

          futParentInsert.flatMap { _ =>
            findTransactionTypes(ids = Some(Seq(id))).map { result =>
              result.map(_.items.headOption) match {
                case Validation.Success(Some(transactionType)) => transactionType
                case _ => throw new ResourceUnexpectedlyNotFound("TransactionGroup", id)
              }
            }
          }
        }
      }
    }
  }

  def adjustTransactionType(
    guid: UUID,
    optParentGuid: Option[UUID],
    optName: Option[String],
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[Validation[TransactionType]] = {
    val transactionTypeQuery = TransactionTypes.filter { r => r.guid === guid && r.deletedAt.isEmpty }

    val futValTransactionType = database.run(transactionTypeQuery.result).map { transactionTypes =>
      transactionTypes.headOption.map { transactionType =>
        transactionType.ownershipKey match {
          case TransactionTypeOwnership.System.key => Validation.failure("guid", "useless.error.systemTransactionType")
          case TransactionTypeOwnership.User.key => Validation.success(transactionType)
          case key => throw new RuntimeException(s"unknown enum key [${key}]")
        }
      }.getOrElse {
        Validation.failure("guid", "useless.error.unknownGuid", "specified" -> guid.toString)
      }
    }

    val futValOptParentId = optParentGuid.map { parentGuid =>
      val transactionTypeQuery = TransactionTypes.filter { r => r.guid === parentGuid && r.deletedAt.isEmpty }
      database.run(transactionTypeQuery.result).map { transactionTypes =>
        transactionTypes.headOption.map { transactionType =>
          Validation.success(Some(transactionType.id))
        }.getOrElse {
          Validation.failure("parentGuid", "useless.error.unknownGuid", "specified" -> parentGuid.toString)
        }
      }
    }.getOrElse {
      Future.successful(Validation.success(None))
    }

    futValTransactionType.flatMap { valTransactionType =>
      futValOptParentId.flatMap { valOptParentId =>
        ValidationUtil.future(valTransactionType ++ valOptParentId) { case (transactionType, optParentId) =>
          if (optName.isEmpty || optName == Some(transactionType.name)) {
            optParentId.map { parentId =>
              changeTransactionTypeParent(transactionType, parentId, accessToken)
            }.getOrElse {
              records2models(Seq(transactionType)).map { transactionTypes =>
                transactionTypes.headOption.getOrElse {
                  throw new ResourceUnexpectedlyNotFound("TransactionType", transactionType.id)
                }
              }
            }
          } else {
            replaceTransactionType(transactionType, optParentId, optName.get, accessToken)
          }
        }
      }
    }
  }

  private def changeTransactionTypeParent(
    transactionType: TransactionTypeRecord,
    parentId: Long,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[TransactionType] = {
    val delete = TransactionTypeSubtypes.filter { tts =>
      tts.childTransactionTypeId === transactionType.id &&
      tts.deletedAt.isEmpty
    }.map { tts =>
      (tts.deletedAt, tts.deletedByAccount, tts.deletedByAccessToken)
    }.update((Some(SqlUtil.now), Some(accessToken.resourceOwner.guid), Some(accessToken.guid)))

    val transactionTypeSubtypes = TransactionTypeSubtypes.map { r =>
      (r.parentTransactionTypeId, r.childTransactionTypeId, r.createdByAccount, r.createdByAccessToken)
    }.returning(TransactionTypeSubtypes.map(_.id))

    val insert = transactionTypeSubtypes += ((parentId, transactionType.id, accessToken.resourceOwner.guid, accessToken.guid))

    val transaction = delete.andThen(insert).transactionally

    database.run(transaction).flatMap { result =>
      findTransactionTypes(ids = Some(Seq(transactionType.id))).map { result =>
        result.map(_.items.headOption) match {
          case Validation.Success(Some(transactionType)) => transactionType
          case _ => throw new ResourceUnexpectedlyNotFound("TransactionType", transactionType.id)
        }
      }
    }
  }

  private def replaceTransactionType(
    oldTransactionType: TransactionTypeRecord,
    optParentId: Option[Long],
    name: String,
    accessToken: AccessToken
  )(implicit ec: ExecutionContext): Future[TransactionType] = {
    // First, create the new TransactionType,
    val transactionTypes = TransactionTypes.map { r =>
      (r.guid, r.name, r.ownershipKey, r.createdByAccount, r.createdByAccessToken)
    }.returning(TransactionTypes.map(_.id))

    // preserving the old ownership key.
    val insertTransactionType = transactionTypes +=
      ((UUID.randomUUID, name, oldTransactionType.ownershipKey, accessToken.resourceOwner.guid, accessToken.guid))

    database.run(insertTransactionType).flatMap { newTransactionTypeId =>
      // If a parent ID is specified, use it -
      optParentId.map { parentId =>
        Future.successful((parentId, None))
      }.getOrElse {
        // otherwise, use the old TransactionType's parent.
        val query = TransactionTypeSubtypes.filter { r =>
          r.childTransactionTypeId === oldTransactionType.id && r.deletedAt.isEmpty
        }

        database.run(query.result).map { transactionTypeSubtypes =>
          transactionTypeSubtypes.headOption.map { transactionTypeSubtype =>
            (transactionTypeSubtype.parentTransactionTypeId, Some(transactionTypeSubtype.id))
          }.getOrElse {
            throw new ResourceUnexpectedlyNotFound("Parent TransactionType", oldTransactionType.id)
          }
        }
      }.flatMap { case (parentTransactionTypeId, optTransactionTypeSubtypeId) =>
        // Now that we have the parent ID, create the appropriate
        // TransactionTypeSubtype record for the new TransactionType.

        // Notice that we don't delete the old TransactionType's
        // TransactionTypeSubtype because that relationship is technically
        // still valid.
        val transactionTypeSubtypes = TransactionTypeSubtypes.map { r =>
          (r.parentTransactionTypeId, r.childTransactionTypeId, r.adjustedTransactionTypeSubtypeId, r.createdByAccount, r.createdByAccessToken)
        }.returning(TransactionTypeSubtypes.map(_.id))

        val insert = transactionTypeSubtypes +=
          ((parentTransactionTypeId, newTransactionTypeId, optTransactionTypeSubtypeId, accessToken.resourceOwner.guid, accessToken.guid))

        database.run(insert)
      }.flatMap { _ =>
        // Get all the Transactions that belonged to the old TransactionType
        // (or, more specifically, the TransactionTransactionType records)
        val query = TransactionTransactionTypes.filter { r =>
          r.transactionTypeId === oldTransactionType.id && r.deletedAt.isEmpty
        }

        database.run(query.result).flatMap { transactionTransactionTypes =>
          // Now, we have to do the following in a DB transaction:
          // a. insert new TransactionTransactionTypes that associate the old
          //    TransactionType's Transactions with the new TransactionType,
          val insertProjection = TransactionTransactionTypes.map { r =>
            (r.transactionId, r.transactionTypeId, r.adjustedTransactionTransactionTypeId, r.createdByAccount, r.createdByAccessToken)
          }.returning(TransactionTransactionTypes.map(_.id))

          val insertTransactionTransactionTypes = insertProjection ++= transactionTransactionTypes.map { transactionTransactionType =>
            (transactionTransactionType.transactionId, newTransactionTypeId, Some(transactionTransactionType.id), accessToken.resourceOwner.guid, accessToken.guid)
          }

          // b. soft-delete the TransactionTransactionTypes that associated the
          //    old TransactionType with its (former) Transactions
          val deleteTransactionTransactionTypes = TransactionTransactionTypes.filter { r =>
            (r.id inSet transactionTransactionTypes.map(_.id)) && r.deletedAt.isEmpty
          }.map { r =>
            (r.deletedAt, r.deletedByAccount, r.deletedByAccessToken)
          }.update((Some(SqlUtil.now), Some(accessToken.resourceOwner.guid), Some(accessToken.guid)))

          // c. soft-delete the old TransactionType.
          val deleteTransactionType = TransactionTypes.filter { tt =>
            tt.id === oldTransactionType.id && tt.deletedAt.isEmpty
          }.map { tt =>
            (tt.deletedAt, tt.deletedByAccount, tt.deletedByAccessToken)
          }.update((Some(SqlUtil.now), Some(accessToken.resourceOwner.guid), Some(accessToken.guid)))

          val transaction = insertTransactionTransactionTypes.
            andThen(deleteTransactionTransactionTypes).
            andThen(deleteTransactionType).
            transactionally

          // Run the DB transaction
          database.run(transaction).flatMap { result =>
            // and load the full model for the new TransactionType.
            findTransactionTypes(ids = Some(Seq(newTransactionTypeId))).map { result =>
              result.map(_.items.headOption) match {
                case Validation.Success(Some(transactionType)) => transactionType
                case _ => throw new ResourceUnexpectedlyNotFound("TransactionType", newTransactionTypeId)
              }
            }
          }
        }
      }
    }
  }

}
