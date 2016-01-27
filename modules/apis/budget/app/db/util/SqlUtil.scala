package db.budget.util

import java.util.Date
import java.sql

object SqlUtil {

  val now = new sql.Timestamp((new Date).getTime)

}
