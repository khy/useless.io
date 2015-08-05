package services.haiku

import java.util.UUID
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import org.joda.time.DateTime
import reactivemongo.bson.BSONDocument
import reactivemongo.api.QueryOpts
import reactivemongo.core.commands.Count
import io.useless.Message
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
import lib.haiku.{TwoPhaseLineSyllableCounter, Validation}

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
  ): Future[Either[Message, PaginatedResult[Haiku]]] = {
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

  def create(
    inResponseToGuid: Option[UUID],
    lines: Seq[String],
    createdBy: User
  ): Future[Validation[Haiku]] = {
    val valLines = validate(lines)

    val futValOptInResponseTo: Future[Validation[Option[ShallowHaiku]]] = inResponseToGuid.map { inResponseToGuid =>
      getShallowHaikus(Seq(inResponseToGuid)).map { shallowHaikus =>
        shallowHaikus.headOption.map { shallowHaiku =>
          Validation.success(Some(shallowHaiku))
        }.getOrElse {
          Validation.failure("inResponseToGuid", "useless.haiku.error.nonExistantHaikuGuid", "guid" -> inResponseToGuid.toString)
        }
      }
    }.getOrElse {
      Future.successful(Validation.success(None))
    }

    futValOptInResponseTo.flatMap { valOptInResponseTo =>
      Validation.future(valLines ++ valOptInResponseTo) { case (lines, optInResponseTo) =>
        val document = new HaikuDocument(
          guid = UUID.randomUUID,
          inResponseToGuid = inResponseToGuid,
          lines = lines,
          createdByGuid = createdBy.guid,
          createdAt = DateTime.now
        )

        collection.insert(document).map { lastError =>
          if (lastError.ok) {
            Haiku(document.guid, optInResponseTo, document.lines, document.createdAt, createdBy)
          } else {
            throw lastError
          }
        }
      }
    }
  }

  lazy val counter = TwoPhaseLineSyllableCounter.default()

  private def validate(lines: Seq[String]): Validation[Seq[String]] = {
    def validateLine(index: Int, expectedSyllables: Int): Validation[String] = {
      val lineKey = "line" + (index + 1).toString

      lines.lift(index).map { line =>
        counter.count(line).map { syllables =>
          if ((syllables.min - 2) > expectedSyllables) {
            Validation.failure(lineKey, "useless.haiku.error.tooManySyllables",
              "expected" -> expectedSyllables.toString, "actualLow" -> syllables.min.toString, "actualHigh" -> syllables.max.toString
            )
          } else if ((syllables.max + 1) < expectedSyllables) {
            Validation.failure(lineKey, "useless.haiku.error.tooFewSyllables",
              "expected" -> expectedSyllables.toString, "actualLow" -> syllables.min.toString, "actualHigh" -> syllables.max.toString
            )
          } else {
            Validation.success(line)
          }
        }.getOrElse {
          Validation.success(line)
        }
      }.getOrElse {
        Validation.failure(lineKey, "useless.haiku.error.missingLine")
      }
    }

    val lineOne = validateLine(0, 5)
    val lineTwo = validateLine(1, 7)
    val lineThree = validateLine(2, 5)

    (lineOne ++ lineTwo ++ lineThree).map { case ((one, two), three) =>
      Seq(one, two, three)
    }
  }

}
