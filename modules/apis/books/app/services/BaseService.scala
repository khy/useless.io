package services.books

import scala.concurrent.Future
import play.api.Play
import play.api.db.DB
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.PostgresDriver.simple.Database
import slick.driver.PostgresDriver.backend.Session

object BaseService {

  def scrubTsQuery(raw: String) = {
    raw.trim.
      replaceAll(" +", " ").
      replace("!", "").replace("|", "").replace("&", "").
      split(" ").toSeq.map(_ + ":*").mkString("|")
  }

}

trait BaseService {

  protected lazy val database = Database.forConfig("db.books", Play.current.configuration.underlying)

  protected def withDbSession[T](query: Session => T): Future[T] = Future {
    database.withSession(query(_))
  }

  protected def withDynamicDbSession[T](query: => T): Future[T] = Future {
    database.withDynSession(query)
  }

}
