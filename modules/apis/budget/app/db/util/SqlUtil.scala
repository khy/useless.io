package db.budget.util

import java.util.Date
import java.sql

object SqlUtil {

  def now() = new sql.Timestamp((new Date).getTime)

}
