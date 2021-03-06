package services.haiku

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import play.api.Application
import play.api.libs.ws.WS
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
    AccountClient.instance(
      client = WS.client,
      baseUrl = app.configuration.underlying.getString("useless.core.baseUrl"),
      authGuid = app.configuration.underlying.getUuid("haiku.accessTokenGuid")
    )
  }

  private val paginationConfig = PaginationParams.defaultPaginationConfig.copy(
    defaultStyle = PrecedenceBasedPagination
  )

  def haikuLines(record: HaikuRecord): Seq[String] = {
    Seq(record.lineOne, record.lineTwo, record.lineThree)
  }

  def db2model(records: Seq[HaikuRecord])(implicit app: Application, ec: ExecutionContext): Future[Seq[Haiku]] = {
    val inResponseToIds = records.map(_.inResponseToId).filter(_.isDefined).map(_.get)
    val futInResponseToRecords = database.run(Haikus.filter(_.id inSet inResponseToIds).result)

    for {
      inResponseToRecords1 <- futInResponseToRecords
      haikus <- {
        val allRecords = (records ++ inResponseToRecords1)
        val inResponseToIds = inResponseToRecords1.map(_.inResponseToId).filter(_.isDefined).map(_.get)
        val futInResponseToRecords2 = database.run(Haikus.filter(_.id inSet inResponseToIds).result)

        val responseCountQuery = Haikus.filter { haiku =>
          haiku.inResponseToId inSet allRecords.map(_.id)
        }.groupBy { haiku =>
          haiku.inResponseToId
        }.map { case (haikuId, group) =>
          (haikuId, group.length)
        }

        val createdByAccountGuids = allRecords.map(_.createdByAccount)
        val futAccounts = getUsers(createdByAccountGuids)

        for {
          inResponseToRecords2 <- futInResponseToRecords2
          responseCounts <- database.run(responseCountQuery.result)
          accounts <- futAccounts
        } yield records.map { record =>
          def responseCount(record: HaikuRecord) = responseCounts.
            find { case (haikuId, _) => haikuId == Some(record.id) }.
            map { case (_, count) => count }.
            getOrElse(0)

          def createdBy(record: HaikuRecord) = accounts.find { account =>
            account.guid == record.createdByAccount
          }.getOrElse(AnonUser)

          def shallowHaiku(record: HaikuRecord) = {
            val inResponseToGuid = inResponseToRecords2.filter { inResponseToRecord =>
              Some(inResponseToRecord.id) == record.inResponseToId
            }.map(_.guid).headOption

            ShallowHaiku(record.guid, haikuLines(record), record.attribution, inResponseToGuid, responseCount(record), new DateTime(record.createdAt), createdBy(record))
          }

          val inResponseTo = inResponseToRecords1.filter { inResponseToRecord =>
            Some(inResponseToRecord.id) == record.inResponseToId
          }.map(shallowHaiku).headOption

          Haiku(record.guid, haikuLines(record), record.attribution, inResponseTo, responseCount(record), new DateTime(record.createdAt), createdBy(record))
        }
      }
    } yield haikus
  }

  def find(
    ids: Option[Seq[Long]] = None,
    guids: Option[Seq[UUID]] = None,
    userHandles: Option[Seq[String]] = None,
    inResponseToGuids: Option[Seq[UUID]] = None,
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

        guids.foreach { guids =>
          query = query.filter { _.guid inSet guids }
        }

        optAccounts.foreach { accounts =>
          query = query.filter { _.createdByAccount inSet accounts.map(_.guid) }
        }

        inResponseToGuids.foreach { inResponseToGuids =>
          val inResponseToIds = Haikus.filter(_.guid inSet inResponseToGuids).map(_.id)
          query = query.filter { _.inResponseToId in inResponseToIds }
        }

        var pagedQuery = query.sortBy(_.createdAt.desc)

        pagedQuery = paginationParams match {
          case params: OffsetBasedPaginationParams[_] => pagedQuery.drop(params.offset)
          case params: PrecedenceBasedPaginationParams[_] => params.after.map { after =>
            pagedQuery.filter {
              _.createdAt < Haikus.filter(_.guid === after).map(_.createdAt).min
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
