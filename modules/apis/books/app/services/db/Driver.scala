package services.books.db

import com.github.tminglei.slickpg._

trait Driver
  extends ExPostgresDriver
  with PgSearchSupport
{

  object Api
    extends API
    with SearchImplicits
    with SearchAssistants

  override val api = Api

}

object Driver extends Driver
