package services.haiku

import java.util.UUID
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
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
import io.useless.validation._

import db.haiku._
import models.haiku._
import models.haiku.mongo.HaikuMongo._
import lib.haiku.TwoPhaseLineSyllableCounter

object HaikuService extends Configuration {

  private val CollectionName = "haikus"

  private lazy val collection = {
    MongoAccessor("haiku.mongo.uri").collection(CollectionName)
  }

  private lazy val database = Database.forConfig("db.haiku")

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

  //TODO: Execution contexts!

  def haikuLines(record: HaikuRecord): Seq[String] = {
    Seq(record.lineOne, record.lineTwo, record.lineThree)
  }

  def db2model(records: Seq[HaikuRecord]): Future[Seq[Haiku]] = {
    find(ids = Some(records.map(_.inResponseToId))).flatMap { inResponseToResult1 =>
      val inResponseToRecords1 = inResponseToResult1.toSuccess.value.items
      val futInResponseToRecords2 = find(ids = Some(inResponseToRecords1.map(_.inResponseToId))).map { records =>
        records.toSuccess.value.items
      }

      val createdByAccountGuids = (records ++ inResponseToRecords1).map(_.createdByAccount)
      val futAccounts = getUsers(createdByAccountGuids)

      for {
        inResponseToRecords2 <- futInResponseToRecords2
        accounts <- futAccounts
      } yield records.map { record =>
        def createdBy(record: HaikuRecord) = accounts.find { account =>
          account.guid == record.createdByAccount
        }.getOrElse(AnonUser)

        val inResponseTo = inResponseToRecords1.filter { inResponseToRecord =>
          inResponseToRecord.id == record.inResponseToId
        }.map { record =>
          val inResponseToGuid = inResponseToRecords2.filter { inResponseToRecord =>
            inResponseToRecord.id == record.inResponseToId
          }.map(_.guid).headOption

          ShallowHaiku(record.guid, inResponseToGuid, haikuLines(record), new DateTime(record.createdAt), createdBy(record))
        }.headOption

        Haiku(record.guid, inResponseTo, haikuLines(record), new DateTime(record.createdAt), createdBy(record))
      }
    }
  }

  def find(
    ids: Option[Seq[Long]] = None,
    userHandles: Option[Seq[String]] = None,
    rawPaginationParams: RawPaginationParams = RawPaginationParams()
  ): Future[Validation[PaginatedResult[HaikuRecord]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams, paginationConfig)

    ValidationUtil.mapFuture(valPaginationParams) { paginationParams =>
      val futAccounts = userHandles.map { userHandles =>
        val accountFuts = userHandles.map { userHandle => accountClient.getAccountForHandle(userHandle) }
        Future.sequence(accountFuts).map { accounts =>
          Some(accounts.filter(_.isDefined).map(_.get))
        }
      }.getOrElse { Future.successful(None) }

      futAccounts.flatMap { optAccounts =>
        var query: Query[HaikusTable, HaikuRecord, Seq] = Haikus

        optAccounts.foreach { accounts =>
          query.filter { _.createdByAccount inSet accounts.map(_.guid) }
        }

        database.run(query.result).map { haikuRecords =>
          PaginatedResult.build(haikuRecords, paginationParams, None)
        }
      }
    }
  }

  private val AnonUser: User = new PublicUser(
    guid = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    handle = "anon",
    name = None
  )

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
      ValidationUtil.mapFuture(valLines ++ valOptInResponseTo) { case (lines, optInResponseTo) =>
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
