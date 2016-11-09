package db.workouts

import com.github.tminglei.slickpg._

trait Driver
  extends ExPostgresDriver
  with PgDate2Support
  with PgPlayJsonSupport
{

  def pgjson = "jsonb"

  object Api
    extends API
    with DateTimeImplicits
    with JsonImplicits

  override val api = Api

}

object Driver extends Driver
