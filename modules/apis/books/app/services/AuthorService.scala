package services.books

import java.util.UUID
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.accesstoken.AccessToken

import db.Driver.api._
import db.{Authors, AuthorRecord, AuthorsTable}
import models.books.Author

object AuthorService extends BaseService {

  def getAuthor(guid: UUID): Future[Option[Author]] = {
    val query = Authors.filter(_.guid === guid)

    database.run(query.result).map { results =>
      results.headOption.map { author =>
        Author(author.guid, author.name)
      }
    }
  }

  def findAuthors(
    names: Option[Seq[String]]
  ): Future[Seq[Author]] =  {
    var query: Query[AuthorsTable, AuthorRecord, Seq] = Authors

    names.foreach { names =>
      names.foreach { name =>
        query = query.filter { author =>
          toTsVector(author.name) @@ toTsQuery(BaseService.scrubTsQuery(name))
        }
      }
    }

    database.run(query.result).map { results =>
      results.map { author =>
        Author(author.guid, author.name)
      }
    }
  }

  def addAuthor(
    name: String,
    accessToken: AccessToken
  ): Future[Author] = {
    val authorQuery = Authors.filter(_.name === name)
    database.run(authorQuery.result).flatMap { authors =>
      authors.headOption.map { author =>
        Future.successful(author.guid)
      }.getOrElse {
        insertAuthor(name, accessToken)
      }
    }.flatMap { guid =>
      getAuthor(guid).map { optAuthor =>
        optAuthor.getOrElse {
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
