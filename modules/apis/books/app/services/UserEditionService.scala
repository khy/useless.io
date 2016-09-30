package services.books

import java.util.UUID
import java.sql.Date
import scala.concurrent.{ExecutionContext, Future}
import slick.backend.DatabaseConfig
import io.useless.typeclass.Identify
import io.useless.pagination._
import io.useless.validation._

import models.books.UserEdition
import services.books.db.{Driver, DogEars, EditionCache}

class UserEditionService(
  dbConfig: DatabaseConfig[Driver],
  editionService: EditionService
) {

  import dbConfig.db
  import dbConfig.driver.api._

  implicit val userEditionIdentify = new Identify[UserEdition] {
    def identify(userEdition: UserEdition) = userEdition.edition.isbn
  }

  private val paginationConfig = PaginationParams.defaultPaginationConfig.copy(
    validOrders = Seq("lastDogEaredAt"),
    defaultOrder = "lastDogEaredAt"
  )

  def findUserEditions(
    userGuids: Option[Seq[UUID]],
    rawPaginationParams: RawPaginationParams = RawPaginationParams()
  )(implicit ec: ExecutionContext): Future[Validation[PaginatedResult[UserEdition]]] = {
    val valPaginationParams = PaginationParams.build(rawPaginationParams, paginationConfig)

    ValidationUtil.mapFuture(valPaginationParams) { paginationParams =>
      var dogEarsQuery = DogEars.groupBy { dogEar =>
        (dogEar.isbn, dogEar.createdByAccount)
      }.map { case ((isbn, createdByAccount), group) =>
        (isbn, createdByAccount, group.map(_.createdAt).max)
      }

      userGuids.foreach { userGuids =>
        dogEarsQuery = dogEarsQuery.filter { case (_, createdByAccount, _) =>
          createdByAccount inSet userGuids
        }
      }

      val pagedDogEarsQuery = dogEarsQuery.sortBy { case (_, _, lastCreatedAt) =>
        lastCreatedAt
      }.take(paginationParams.limit)

      db.run(pagedDogEarsQuery.result).flatMap { results =>
        val isbns = results.map { case (isbn, _, _) => isbn }
        val editionQuery = EditionCache.filter(_.isbn inSet isbns)

        db.run(editionQuery.result).map { editionRecords =>
          val userEditions = editionService.db2api(editionRecords).map { edition =>
            UserEdition(edition)
          }
          PaginatedResult.build(userEditions, paginationParams)
        }
      }
    }
  }

}
