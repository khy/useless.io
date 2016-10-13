package services.budget

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}
import play.api.Application
import slick.driver.PostgresDriver.api._
import org.joda.time.DateTime
import io.useless.accesstoken.AccessToken
import io.useless.exception.service._
import io.useless.pagination._
import io.useless.validation._

import models.budget.{TransactionType, TransactionTypeOwnership}
import db.budget._
import db.budget.util.DatabaseAccessor
import db.budget.util.SqlUtil
import services.budget.util.UsersHelper

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

    val contextIds = records.map(_.contextId).filter(_.isDefined).map(_.get)
    val contextsQuery = Contexts.filter { context =>
      context.id.inSet(contextIds) && context.deletedAt.isEmpty
    }
    val futContexts = database.run(contextsQuery.result)

    for {
      users <- futUsers
      parents <- futParents
      contexts <- futContexts
    } yield {
      records.map { record =>
        TransactionType(
          guid = record.guid,
          contextGuid = contexts.find { context =>
            record.contextId.map { contextId => contextId == contextId }.getOrElse(false)
          }.map(_.guid),
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
    guids: Option[Seq[UUID]] = None,
    contextGuids: Option[Seq[UUID]] = None,
    names: Option[Seq[String]] = None,
    ownerships: Option[Seq[TransactionTypeOwnership]] = None,
    userGuids: Option[Seq[UUID]] = None,
    rawPaginationParams: RawPaginationParams = RawPaginationParams()
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[TransactionType]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams)

    ValidationUtil.mapFuture(valPaginationParams) { paginationParams =>
      var query = TransactionTypes.filter { r => r.deletedAt.isEmpty }

      ids.foreach { ids =>
        query = query.filter { _.id inSet ids }
      }

      guids.foreach { guids =>
        query = query.filter { _.guid inSet guids }
      }

      contextGuids.foreach { contextGuids =>
        val subQuery = Contexts.filter { context =>
          context.guid.inSet(contextGuids) && context.deletedAt.isEmpty
        }.map(_.id)

        query = query.filter { transactionType =>
          transactionType.contextId.in(subQuery) ||
          transactionType.ownershipKey === TransactionTypeOwnership.System.key
        }
      }

      names.foreach { names =>
        query = query.filter { _.name inSet names }
      }

      ownerships.foreach { ownerships =>
        query = query.filter { _.ownershipKey inSet ownerships.map(_.key)}
      }

      userGuids.foreach { userGuids =>
        val subQuery = ContextUsers.filter { contextUser =>
          contextUser.userGuid.inSet(userGuids) && contextUser.deletedAt.isEmpty
        }.map(_.contextId)

        query = query.filter { transactionType =>
          transactionType.contextId.in(subQuery) ||
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
    contextGuid: UUID,
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

    val contextQuery = Contexts.filter { _.guid === contextGuid }
    val futValContextId = database.run(contextQuery.result).map { contexts =>
      contexts.headOption.map { context =>
        Validation.success(context.id)
      }.getOrElse {
        Validation.failure("contextGuid", "useless.error.unknownGuid", "specified" -> contextGuid.toString)
      }
    }

    val futValName = if (name.isEmpty) {
      Future.successful(Validation.failure("name", "useless.error.cannotBeEmpty"))
    } else {
      val nameQuery = TransactionTypes.join(Contexts).on { case (txnType, context) =>
        txnType.contextId === context.id
      }.filter { case (txnType, context) =>
        txnType.name === name && context.guid === contextGuid
      }.map { case (txnType, _) => txnType }

      database.run(nameQuery.result).map { txnType =>
        if (txnType.headOption.isDefined) {
          Validation.failure("name", "useless.error.duplicate", "specified" -> name)
        } else {
          Validation.success(name)
        }
      }
    }

    for {
      valOptParentId <- futValOptParentId
      valContextId <- futValContextId
      valName <- futValName
      valTransactionType <- {
        ValidationUtil.mapFuture(valOptParentId ++ valContextId ++ valName) { case ((optParentId, contextId), name) =>
          val transactionTypes = TransactionTypes.map { r =>
            (r.guid, r.contextId, r.name, r.ownershipKey, r.createdByAccount, r.createdByAccessToken)
          }.returning(TransactionTypes.map(_.id))

          val insert = transactionTypes += ((UUID.randomUUID, Some(contextId), name, ownership.key, accessToken.resourceOwner.guid, accessToken.guid))

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
                  case _ => throw new ResourceNotFound("TransactionGroup", id)
                }
              }
            }
          }
        }
      }
    } yield valTransactionType
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
        ValidationUtil.mapFuture(valTransactionType ++ valOptParentId) { case (transactionType, optParentId) =>

          val optUpdateName = optName.filter { name =>
            name != transactionType.name
          }.map { name =>
            TransactionTypes.filter(_.id === transactionType.id).map(_.name).update(name)
          }

          val optChangeParent = optParentId.map { parentId =>
            TransactionTypeSubtypes.filter { tts =>
             tts.childTransactionTypeId === transactionType.id && tts.deletedAt.isEmpty
           }.result.flatMap { transactionTypeSubtypes =>
             val removeOldParent = TransactionTypeSubtypes.filter { tts =>
               tts.id inSet transactionTypeSubtypes.map(_.id)
             }.map { tts =>
               (tts.deletedAt, tts.deletedByAccount, tts.deletedByAccessToken)
             }.update((Some(SqlUtil.now), Some(accessToken.resourceOwner.guid), Some(accessToken.guid)))

             val insertProjection = TransactionTypeSubtypes.map { r =>
               (r.parentTransactionTypeId, r.childTransactionTypeId, r.createdByAccount, r.createdByAccessToken)
             }.returning(TransactionTypeSubtypes.map(_.id))

             val insertNewParent = insertProjection +=
               ((parentId, transactionType.id, accessToken.resourceOwner.guid, accessToken.guid))

             removeOldParent andThen insertNewParent
           }
          }

          val dbTransaction = DBIO.sequence(optUpdateName.toSeq ++ optChangeParent.toSeq).transactionally

          database.run(dbTransaction).flatMap { result =>
            findTransactionTypes(ids = Some(Seq(transactionType.id))).map { result =>
              result.map(_.items.headOption) match {
                case Validation.Success(Some(transactionType)) => transactionType
                case _ => throw new ResourceNotFound("TransactionType", transactionType.id)
              }
            }
          }
        }
      }
    }
  }

}
