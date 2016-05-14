package services.haiku

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import play.api.Application
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import io.useless.Message
import io.useless.accesstoken.AccessToken
import io.useless.account.{User, PublicUser}
import io.useless.client.account.AccountClient
import io.useless.util.configuration.Configuration
import io.useless.util.configuration.RichConfiguration._
import io.useless.pagination._
import io.useless.validation._

import db.haiku._
import models.haiku._
import lib.haiku.TwoPhaseLineSyllableCounter

object HaikuService extends Configuration {

  val database = Database.forConfig("db.haiku")

  def accountClient(implicit app: Application) = {
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

  def db2model(records: Seq[HaikuRecord])(implicit app: Application, ec: ExecutionContext): Future[Seq[Haiku]] = {
    val inResponseToIds = records.map(_.inResponseToId).filter(_.isDefined).map(_.get)
    find(ids = Some(inResponseToIds)).flatMap { inResponseToResult1 =>
      val inResponseToRecords1 = inResponseToResult1.toSuccess.value.items
      val inResponseToIds = inResponseToRecords1.map(_.inResponseToId).filter(_.isDefined).map(_.get)
      val futInResponseToRecords2 = find(ids = Some(inResponseToIds)).map { records =>
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
          Some(inResponseToRecord.id) == record.inResponseToId
        }.map { record =>
          val inResponseToGuid = inResponseToRecords2.filter { inResponseToRecord =>
            Some(inResponseToRecord.id) == record.inResponseToId
          }.map(_.guid).headOption

          ShallowHaiku(record.guid, inResponseToGuid, haikuLines(record), record.attribution, new DateTime(record.createdAt), createdBy(record))
        }.headOption

        Haiku(record.guid, inResponseTo, haikuLines(record), record.attribution, new DateTime(record.createdAt), createdBy(record))
      }
    }
  }

  def find(
    ids: Option[Seq[Long]] = None,
    userHandles: Option[Seq[String]] = None,
    rawPaginationParams: RawPaginationParams = RawPaginationParams()
  )(implicit app: Application, ec: ExecutionContext): Future[Validation[PaginatedResult[HaikuRecord]]] = {
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

        ids.foreach { ids =>
          query = query.filter { _.id inSet ids }
        }

        optAccounts.foreach { accounts =>
          query = query.filter { _.createdByAccount inSet accounts.map(_.guid) }
        }

        var pagedQuery = query.sortBy(_.createdAt.desc)

        pagedQuery = paginationParams match {
          case params: OffsetBasedPaginationParams => pagedQuery.drop(params.offset)
          case params: PrecedenceBasedPaginationParams => params.after.map { after =>
            pagedQuery.filter {
              _.createdAt < Haikus.filter(_.guid === params.after).map(_.createdAt).min
            }
          }.getOrElse { pagedQuery }
        }

        pagedQuery = pagedQuery.take(paginationParams.limit)

        val futCount = database.run(query.length.result)
        val futHaikuRecords = database.run(pagedQuery.result)

        for {
          count <- futCount
          haikuRecords <- futHaikuRecords
        } yield PaginatedResult.build(haikuRecords, paginationParams, Some(count))
      }
    }
  }

  private val AnonUser: User = new PublicUser(
    guid = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    handle = "anon",
    name = None
  )

  private def getUsers(guids: Seq[UUID])(implicit app: Application, ec: ExecutionContext): Future[Seq[User]] = {
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
    attribution: Option[String],
    accessToken: AccessToken
  )(implicit app: Application, ec: ExecutionContext): Future[Validation[HaikuRecord]] = {
    val valLines = validate(lines)

    val futValOptInResponseToId: Future[Validation[Option[Long]]] = inResponseToGuid.map { inResponseToGuid =>
      val query = Haikus.filter { _.guid === inResponseToGuid }
      database.run(query.result).map { haikuRecords =>
        haikuRecords.headOption.map { haikuRecord =>
          Validation.success(Some(haikuRecord.id))
        }.getOrElse {
          Validation.failure("inResponseToGuid", "useless.haiku.error.nonExistantHaikuGuid", "guid" -> inResponseToGuid.toString)
        }
      }
    }.getOrElse {
      Future.successful(Validation.success(None))
    }

    futValOptInResponseToId.flatMap { valOptInResponseToId =>
      ValidationUtil.mapFuture(valLines ++ valOptInResponseToId) { case (lines, optInResponseToId) =>
        val haikus = Haikus.map { r =>
          (r.guid, r.lineOne, r.lineTwo, r.lineThree, r.inResponseToId, r.attribution, r.createdByAccount, r.createdByAccessToken)
        }.returning(Haikus.map(_.id))

        val insert = haikus += (UUID.randomUUID, lines(0), lines(1), lines(2), optInResponseToId, attribution, accessToken.resourceOwner.guid, accessToken.guid)

        database.run(insert).flatMap { id =>
          find(ids = Some(Seq(id))).map { result =>
            result.map(_.items.headOption) match {
              case Validation.Success(Some(haiku)) => haiku
              case _ => throw new RuntimeException("Could not find haiku " + id.toString)
            }
          }
        }
      }
    }
  }

  private def validate(lines: Seq[String])(implicit app: Application): Validation[Seq[String]] = {
    lazy val counter = TwoPhaseLineSyllableCounter.default()

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
