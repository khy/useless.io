package test.util

import org.scalatest._

trait ClearDbBeforeEach {

  self: BeforeAndAfterEach with BeforeAndAfterAll =>

  override def beforeEach = {
    DbUtil.clearDb()
  }

}
