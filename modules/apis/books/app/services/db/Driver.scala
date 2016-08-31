package services.books.db

import com.github.tminglei.slickpg._

trait Driver
  extends ExPostgresDriver
  with PgArraySupport
  with PgDateSupportJoda
  with PgPlayJsonSupport
  with PgSearchSupport
{

  val pgjson = "jsonb"

  object Api
    extends API
    with ArrayImplicits
    with DateTimeImplicits
    with JsonImplicits
    with SearchImplicits
    with SearchAssistants

  override val api = Api

}

object Driver extends Driver
