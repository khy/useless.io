package services.books

import java.util.UUID
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.accesstoken.AccessToken

import db.Driver.api._
import db.{Authors, AuthorRecord, AuthorsTable}
import models.books.Author

object AuthorService {

  def instance() = {
    new AuthorService()
  }

}

class AuthorService extends BaseService {

  def db2api(records: Seq[AuthorRecord]): Future[Seq[Author]] = Future.successful {
    records.map { author =>
      Author(author.guid, author.name)
    }
  }

  def findAuthors(
    guids: Option[Seq[UUID]] = None,
    names: Option[Seq[String]] = None
  ): Future[Seq[AuthorRecord]] =  {
    var query: Query[AuthorsTable, AuthorRecord, Seq] = Authors

    guids.foreach { guids =>
      query = query.filter { author =>
        author.guid inSet (guids)
      }
    }

    names.foreach { names =>
      names.foreach { name =>
        query = query.filter { author =>
          toTsVector(author.name) @@ toTsQuery(BaseService.scrubTsQuery(name))
        }
      }
    }

    database.run(query.result)
  }

  def addAuthor(
    name: String,
    accessToken: AccessToken
  ): Future[AuthorRecord] = {
    val authorQuery = Authors.filter(_.name === name)
    database.run(authorQuery.result).flatMap { authors =>
      authors.headOption.map { author =>
        Future.successful(author.guid)
      }.getOrElse {
        insertAuthor(name, accessToken)
      }
    }.flatMap { guid =>
      findAuthors(guids = Some(Seq(guid))).map { authors =>
        authors.headOption.getOrElse {
          throw new ResourceUnexpectedlyNotFound("Author", guid)
        }
      }
    }
  }

  private def insertAuthor(
    name: String,
    accessToken: AccessToken
  ): Future[UUID] = {
    val projection = Authors.map { author =>
      (author.guid, author.name, author.createdByAccount, author.createdByAccessToken)
    }.returning(Authors.map(_.guid))

    val insert = projection += (UUID.randomUUID, name, accessToken.resourceOwner.guid, accessToken.guid)

    database.run(insert)
  }

}
