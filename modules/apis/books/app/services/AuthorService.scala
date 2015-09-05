package services.books

import java.util.UUID
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.accesstoken.AccessToken

import db.Driver.api._
import db.Authors
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

  def findAuthors(name: String): Future[Seq[Author]] =  {
    val query = Authors.filter { author =>
      tsVector(author.name) @@ tsQuery(BaseService.scrubTsQuery(name).bind)
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
    findAuthors(name).flatMap { authors =>
      authors.headOption.map { author =>
        Future.successful(author)
      }.getOrElse {
        insertAuthor(name, accessToken).flatMap { newAuthorGuid =>
          getAuthor(newAuthorGuid).map { optAuthor =>
            optAuthor.getOrElse {
              throw new ResourceUnexpectedlyNotFound("Author", newAuthorGuid)
            }
          }
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
