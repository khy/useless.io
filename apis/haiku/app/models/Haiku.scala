package models.haiku

import java.util.UUID
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import scala.collection.mutable
import org.joda.time.DateTime
import reactivemongo.bson.BSONDocument
import io.useless.account.User
import io.useless.reactivemongo.MongoAccessor
import io.useless.client.account.AccountClient
import io.useless.reactivemongo.bson.UuidBson._
import io.useless.reactivemongo.bson.DateTimeBson._
import io.useless.util.{Configuration, Uuid}

import mongo.HaikuMongo._
import lib.haiku.{ Pagination, TwoPhaseLineSyllableCounter }

object Haiku extends Configuration {

  private lazy val collection = {
    MongoAccessor("haiku.mongo.uri").collection("haikus")
  }

  lazy val accountClient = {
    val authGuid = configuration.getString("haiku.accessTokenGuid").flatMap { raw =>
      Uuid.parseUuid(raw).toOption
    }.get

    AccountClient.instance(authGuid)
  }

  def find(optUserHandle: Option[String], pagination: Pagination): Future[Seq[Haiku]] = {
    val count = pagination.count.getOrElse(30)

    userQuery(optUserHandle).flatMap { userQuery =>
      paginationQuery(pagination).flatMap { paginationQuery =>
        val query = userQuery.add(paginationQuery)

        val futureDocuments = collection.find(query).
          sort(BSONDocument("created_at" -> -1)).
          cursor[HaikuDocument].collect[Seq](count)

        futureDocuments.flatMap { documents =>
          val futures = documents.map { document =>
            accountClient.getAccount(document.createdByGuid).map { optAccount =>
              optAccount match {
                case Some(user: User) => new Haiku(user, document)
                case _ => throw new RuntimeException(s"Could not find User for createdByGuid [${document.createdByGuid}]")
              }
            }
          }

          Future.sequence(futures)
        }
      }
    }
  }

  def create(
    user: User,
    lines: Seq[String]
  ): Future[Either[Seq[Option[String]], Haiku]] = {
    val errors = validate(lines)

    if (errors.filter(_.isDefined).length > 0) {
      Future.successful(Left(errors))
    } else {
      val document = new HaikuDocument(
        guid = UUID.randomUUID,
        lines = lines,
        createdByGuid = user.guid,
        createdAt = DateTime.now
      )

      collection.insert(document).map { lastError =>
        if (lastError.ok) {
          Right(new Haiku(user, document))
        } else {
          throw lastError
        }
      }
    }

  }

  private def validate(lines: Seq[String]): Seq[Option[String]] = {
    val counter = TwoPhaseLineSyllableCounter.default

    lines.zip(Seq(5,7,5)).map { case (line, expectedSyllables) =>
      counter.count(line).map { syllables =>
        if ((syllables.min - 2) > expectedSyllables)
          Some("useless.haiku.error.too_many_syllables")
        else if ((syllables.max + 1) < expectedSyllables)
          Some("useless.haiku.error.too_few_syllables")
        else
          None
      }.getOrElse(None)
    }
  }

  private def paginationQuery(pagination: Pagination): Future[BSONDocument] = {
    val futureSinceQuery = pagination.since.map { guid =>
      forGuid(guid).map { optDocument =>
        optDocument.map { document =>
          BSONDocument("created_at" -> BSONDocument("$gt" -> document.createdAt))
        }.getOrElse {
          BSONDocument()
        }
      }
    }.getOrElse {
      Future.successful(BSONDocument())
    }

    val futureUntilQuery = pagination.until.map { guid =>
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

    for {
      sinceQuery <- futureSinceQuery
      untilQuery <- futureUntilQuery
    } yield sinceQuery.add(untilQuery)
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

class Haiku(
  val createdBy: User,
  document: HaikuDocument
) {

  val guid = document.guid

  val lines = document.lines

  val createdAt = document.createdAt

}
