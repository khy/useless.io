package services.books

import db.Driver.api._

object BaseService {

  private lazy val database = Database.forConfig("db.books")

  def scrubTsQuery(raw: String) = {
    raw.trim.
      replaceAll(" +", " ").
      replace("!", "").replace("|", "").replace("&", "").
      split(" ").toSeq.map(_ + ":*").mkString("|")
  }

}

trait BaseService {

  protected lazy val database = BaseService.database

}
