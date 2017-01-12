package db.workouts

import com.github.tminglei.slickpg._

trait Driver
  extends ExPostgresDriver
  with PgDate2Support
  with PgPlayJsonSupport
  with array.PgArrayJdbcTypes
{

  def pgjson = "jsonb"

  object Api
    extends API
    with DateTimeImplicits
    with JsonImplicits
  {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
  }

  override val api = Api

}

object Driver extends Driver
