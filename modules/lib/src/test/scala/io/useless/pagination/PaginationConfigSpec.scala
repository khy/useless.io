package io.useless.pagination

import java.util.UUID
import org.scalatest._
import org.scalatest.OptionValues._

class PaginationConfigSpec
  extends WordSpec
  with MustMatchers
{

  def buildConfig(
    defaultStyle: PaginationStyle = OffsetBasedPagination,
    maxLimit: Int = 100,
    defaultLimit: Int = 20,
    defaultOffset: Int = 0,
    validOrders: Seq[String] = Seq("id"),
    defaultOrder: String = "id"
  ) = PaginationConfig(defaultStyle, maxLimit, defaultLimit, defaultOffset, validOrders, defaultOrder)

  "PaginationConfig" must {

    "not reject a valid configuration" in {
      noException should be thrownBy {
        buildConfig()
      }
    }

    "reject a maxLimit less than 0" in {
      a [IllegalArgumentException] should be thrownBy {
        buildConfig(maxLimit = -1)
      }
    }

    "reject a defaultLimit less than 0" in {
      a [IllegalArgumentException] should be thrownBy {
        buildConfig(defaultLimit = -1)
      }
    }

    "reject a defaultLimit greater than the maxLimit" in {
      a [IllegalArgumentException] should be thrownBy {
        buildConfig(maxLimit = 10, defaultLimit = 20)
      }
    }

    "reject a defaultOffset less than 0" in {
      a [IllegalArgumentException] should be thrownBy {
        buildConfig(defaultOffset = -1)
      }
    }

    "not reject a defaultOffset equal to 0" in {
      noException should be thrownBy {
        buildConfig(defaultOffset = 0)
      }
    }

    "reject defaultOrder not included in validOrders" in {
      a [IllegalArgumentException] should be thrownBy {
        buildConfig(validOrders = Seq("id"), defaultOrder = "count")
      }
    }

  }

}