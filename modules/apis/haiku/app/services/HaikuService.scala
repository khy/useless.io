package services.haiku

import java.util.UUID
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import scala.collection.mutable
import org.joda.time.DateTime
import reactivemongo.bson.BSONDocument
import reactivemongo.api.QueryOpts
import reactivemongo.core.commands.Count
import io.useless.ClientError
import io.useless.account.{User, PublicUser}
import io.useless.reactivemongo.MongoAccessor
import io.useless.client.account.AccountClient
import io.useless.reactivemongo.bson.UuidBson._
import io.useless.reactivemongo.bson.DateTimeBson._
import io.useless.util.configuration.Configuration
import io.useless.util.configuration.RichConfiguration._
import io.useless.pagination._

import models.haiku._
import models.haiku.mongo.HaikuMongo._
import lib.haiku.TwoPhaseLineSyllableCounter

object HaikuService extends Configuration {

  private val CollectionName = "haikus"

  private lazy val collection = {
    MongoAccessor("haiku.mongo.uri").collection(CollectionName)
  }

  lazy val accountClient = {
    val authGuid = configuration.underlying.getUuid("haiku.accessTokenGuid")
    AccountClient.instance(authGuid)
  }

  private val paginationConfig = PaginationConfig(
    defaultStyle = PrecedenceBasedPagination,
    maxLimit = 100,
    defaultLimit = 20,
    defaultOffset = 0,
    validOrders = Seq("created_at"),
    defaultOrder = "created_at"
  )

  def find(
    optUserHandle: Option[String],
    rawPaginationParams: RawPaginationParams
  ): Future[Either[ClientError, PaginatedResult[Haiku]]] = {
    PaginationParams.build(rawPaginationParams, paginationConfig).fold(
      error => Future.successful(Left(error)),
      paginationParams => userQuery(optUserHandle).flatMap { userQuery =>
        paginationQuery(paginationParams).flatMap { paginationQuery =>
          val query = userQuery.add(paginationQuery)

          var queryBuilder = collection.find(query).
            sort(BSONDocument(paginationParams.order -> -1))

          queryBuilder = paginationParams match {
            case obpParams: OffsetBasedPaginationParams => {
              queryBuilder.options(QueryOpts(skipN = obpParams.offset))
            }
            case _ => queryBuilder
          }

          val futCount = collection.db.command(Count(CollectionName, Some(query)))
          val futDocuments = queryBuilder.cursor[HaikuDocument].collect[Seq](paginationParams.limit)

          for {
            count <- futCount
            documents <- futDocuments
            haikus <- buildHaikus(documents)
          } yield {
            Right(PaginatedResult.build(haikus, paginationParams, Some(count)))
          }
        }
      }
    )
  }

  def create(
    inResponseToGuid: Option[UUID],
    lines: Seq[String],
    createdBy: User
  ): Future[Either[Seq[ClientError], Haiku]] = {
    val errors = validate(lines)

    if (!errors.isEmpty) {
      Future.successful(Left(errors))
    } else {
      inResponseToGuid.map { inResponseToGuid =>
        getShallowHaikus(Seq(inResponseToGuid)).map { shallowHaikus =>
          shallowHaikus.headOption.map { shallowHaiku =>
            Right(Some(shallowHaiku))
          }.getOrElse {
            Left(Seq(ClientError("useless.haiku.error.nonExistantHaikuGuid", "guid" -> inResponseToGuid.toString)))
          }
        }
      }.getOrElse {
        Future.successful(Right(None))
      }.flatMap { result =>
        result.fold(
          error => Future.successful(Left(error)),
          optInResponseTo => {
            val document = new HaikuDocument(
              guid = UUID.randomUUID,
              inResponseToGuid = inResponseToGuid,
              lines = lines,
              createdByGuid = createdBy.guid,
              createdAt = DateTime.now
            )

            collection.insert(document).map { lastError =>
              if (lastError.ok) {
                Right(Haiku(document.guid, optInResponseTo, document.lines, document.createdAt, createdBy))
              } else {
                throw lastError
              }
            }
          }
        )
      }
    }

  }

  private val AnonUser: User = new PublicUser(
    guid = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    handle = "anon",
    name = None
  )

  private def buildHaikus(documents: Seq[HaikuDocument]): Future[Seq[Haiku]] = {
    val inResponseToGuids = documents.map(_.inResponseToGuid).filter(_.isDefined).map(_.get)
    val createdByGuids = documents.map(_.createdByGuid)

    val futShallowHaikus = getShallowHaikus(inResponseToGuids)
    val futUsers = getUsers(createdByGuids)

    for {
      shallowHaikus <- futShallowHaikus
      users <- futUsers
    } yield {
      documents.map { document =>
        val inResponseTo = document.inResponseToGuid.flatMap { inResponseToGuid =>
          shallowHaikus.find { shallowHaiku =>
            shallowHaiku.guid == inResponseToGuid
          }
        }

        val createdBy = users.find { user =>
          user.guid == document.createdByGuid
        }.getOrElse(AnonUser)

        Haiku(document.guid, inResponseTo, document.lines, document.createdAt, createdBy)
      }
    }
  }

  private def getShallowHaikus(guids: Seq[UUID]): Future[Seq[ShallowHaiku]] = {
    val query = BSONDocument("_id" -> BSONDocument("$in" -> guids))
    collection.find(query).cursor[HaikuDocument].collect[Seq]().flatMap { documents =>
      val createdByGuids = documents.map(_.createdByGuid)

      getUsers(createdByGuids).map { users =>
        documents.map { document =>
          val createdBy = users.find { user =>
            user.guid == document.createdByGuid
          }.getOrElse(AnonUser)

          ShallowHaiku(document.guid, document.inResponseToGuid, document.lines, document.createdAt, createdBy)
        }
      }
    }
  }

  private def getUsers(guids: Seq[UUID]): Future[Seq[User]] = {
    val userOptFuts = guids.map { guid =>
      accountClient.getAccount(guid).map { optAccount =>
        optAccount match {
          case Some(user: User) => Some(user)
          case _ => None
        }
      }
    }

    Future.sequence(userOptFuts).map { userOpts =>
      userOpts.filter(_.isDefined).map(_.get)
    }
  }

  lazy val counter = TwoPhaseLineSyllableCounter.default()

  private def validate(lines: Seq[String]): Seq[ClientError] = {
    var errors = Seq.empty[ClientError]

    def validateLine(index: Int, expectedSyllables: Int) {
      val line = (index + 1).toString

      if (lines.isDefinedAt(index)) {
        counter.count(lines(index)).foreach { syllables =>
          if ((syllables.min - 2) > expectedSyllables) {
            errors = errors :+ ClientError("useless.haiku.error.tooManySyllables",
              "line" -> line, "expected" -> expectedSyllables.toString, "actualLow" -> syllables.min.toString, "actualHigh" -> syllables.max.toString)
          } else if ((syllables.max + 1) < expectedSyllables) {
            errors = errors :+ ClientError("useless.haiku.error.tooFewSyllables",
              "line" -> line, "expected" -> expectedSyllables.toString, "actualLow" -> syllables.min.toString, "actualHigh" -> syllables.max.toString)
          }
        }
      } else {
        errors = errors :+ ClientError("useless.haiku.error.missingLine", "line" -> line)
      }
    }

    validateLine(0, 5)
    validateLine(1, 7)
    validateLine(2, 5)

    errors
  }

  private def paginationQuery(paginationParams: PaginationParams): Future[BSONDocument] = {
    paginationParams match {
      case precedenceParams: PrecedenceBasedPaginationParams => {
        precedenceParams.after.map { guid =>
          forGuid(guid).map { optDocument =>
            optDocument.map { document =>
              BSONDocument("created_at" -> BSONDocument("$lt" -> document.createdAt))
            }.getOrElse {
              BSONDocument()
            }
          }
        }.getOrElse {
          Future.successful(BSONDocument())
        }
      }

      case offsetParams: OffsetBasedPaginationParams => {
        Future.successful(BSONDocument())
      }
    }
  }

  private def userQuery(optUserHandle: Option[String]): Future[BSONDocument] = {
    optUserHandle.map { userHandle =>
      accountClient.getAccountForHandle(userHandle).map { optAccount =>
        optAccount.map { account =>
          BSONDocument("created_by_guid" -> account.guid)
        }.getOrElse {
          BSONDocument()
        }
      }
    }.getOrElse {
      Future.successful(BSONDocument())
    }
  }

  private def forGuid(guid: UUID): Future[Option[HaikuDocument]] = {
    collection.find(BSONDocument("_id" -> guid)).one[HaikuDocument]
  }

}
