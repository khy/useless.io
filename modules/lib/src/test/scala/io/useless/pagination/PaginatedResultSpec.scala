package io.useless.pagination

import java.util.UUID
import org.scalatest._

import io.useless.typeclass.Identify

class PaginatedResultSpec
  extends WordSpec
  with MustMatchers
{

  case class TestItem(guid: UUID = UUID.randomUUID)
  val items = Seq(TestItem(), TestItem(), TestItem())

  implicit val id = new Identify[TestItem] {
    def identify(ti: TestItem) = ti.guid.toString
  }

  "PaginatedResult.build" must {

    def buildPrecedenceParams(after: UUID = UUID.randomUUID) = {
      val raw = RawPaginationParams(after = Some(after.toString))
      PaginationParams.build(raw).toSuccess.value
    }

    "use the specified items for after params" in {
      val params = buildPrecedenceParams()
      val result = PaginatedResult.build(items, params)
      result.items mustBe(items)
    }

    "use the last UUID in items for the after of the next" in {
      val params = buildPrecedenceParams()
      val result = PaginatedResult.build(items, params)
      result.next.get.after.get mustBe items(2).guid.toString
    }

    "use the ID supplied by Identify for the next after" in {
      val params = buildPrecedenceParams()
      val items = Seq("abc", "def", "ghi")

      implicit val id = new Identify[String] {
        def identify(s: String) = s + "!"
      }

      val result = PaginatedResult.build(items, params)
      result.next.get.after.get mustBe "ghi!"
    }

    "not include a previous for after params" in {
      val params = buildPrecedenceParams()
      val result = PaginatedResult.build(items, params)
      result.previous mustBe(None)
    }

    "use precedence-based pagination, if explicitly specified" in {
      val raw = RawPaginationParams(style = Some(PrecedenceBasedPagination))
      val params = PaginationParams.build(raw).toSuccess.value
      val result = PaginatedResult.build(items, params)
      result.next.get.after.get mustBe items(2).guid.toString
      result.next.get.style mustBe(None)
      result.previous mustBe(None)
    }

    "use offset-based pagination, if explicitly specified" in {
      val raw = RawPaginationParams(style = Some(OffsetBasedPagination))
      val params = PaginationParams.build(raw).toSuccess.value
      val result = PaginatedResult.build(items, params)
      result.first.get.offset.get mustBe(0)
      result.next.get.offset.get mustBe(20)
    }

  }

  def buildOffsetParams[A](
    limit: Option[Int] = None,
    order: Option[String] = None,
    offset: Option[Int] = None,
    page: Option[Int] = None,
    config: PaginationConfig[A] = PaginationParams.defaultPaginationConfig
  ): OffsetBasedPaginationParams[A] = {
    val raw = RawPaginationParams(None, limit, order, offset, page, None)

    PaginationParams.build(raw, config).fold(
      error => throw new RuntimeException(error.toString),
      params => params.asInstanceOf[OffsetBasedPaginationParams[A]]
    )
  }

  "PaginatedResult.pageBased" must {

    "use the specified items" in {
      val params = buildOffsetParams()
      val result = PaginatedResult.pageBased(items, params)
      result.items mustBe(items)
    }

    "include a first page" in {
      val params = buildOffsetParams()
      val result = PaginatedResult.pageBased(items, params)
      result.first.get.page.get mustBe(1)
    }

    "not include a previous page if currently on the first page" in {
      val params = buildOffsetParams(page = Some(1))
      val result = PaginatedResult.pageBased(items, params)
      result.previous mustBe (None)
    }

    "include a previous page" in {
      val params = buildOffsetParams(page = Some(3))
      val result = PaginatedResult.pageBased(items, params)
      result.previous.get.page.get mustBe (2)
    }

    "include a next page" in {
      val params = buildOffsetParams(page = Some(2))
      val result = PaginatedResult.pageBased(items, params)
      result.next.get.page.get mustBe (3)
    }

    "not include a next page if totalItems is specified, and is less than a page away" in {
      val params = buildOffsetParams(limit = Some(10), page = Some(2))
      val result = PaginatedResult.pageBased(items, params, totalItems = Some(18))
      result.next mustBe (None)
    }

    "not include a next page if hasNext is specified false" in {
      val params = buildOffsetParams(page = Some(2))
      val result = PaginatedResult.pageBased(items, params, hasNext = false)
      result.next mustBe (None)
    }

    "not include a last page if totalItems is not specified" in {
      val params = buildOffsetParams(page = Some(2))
      val result = PaginatedResult.pageBased(items, params)
      result.last mustBe (None)
    }

    "include a last page if totalItems is specified" in {
      val params = buildOffsetParams(limit = Some(10), page = Some(2))
      val result = PaginatedResult.pageBased(items, params, totalItems = Some(108))
      result.last.get.page.get mustBe (11)
    }

    "include non-page parameters as-is for all pages, but igoring offset" in {
      val params = buildOffsetParams(
        limit = Some(10), page = Some(5), offset = Some(40), order = Some("rank"),
        config = PaginationParams.defaultPaginationConfig.copy(
          validOrders = Seq("rank", "votes"), defaultOrder = "votes"
        )
      )

      val result = PaginatedResult.pageBased(items, params, totalItems = Some(120))
      Seq(result.first.get, result.previous.get, result.next.get, result.last.get).foreach { params =>
        params.limit.get mustBe (10)
        params.order.get mustBe ("rank")
        params.offset mustBe (None)
      }
    }

  }

  "PaginatedResult.offsetBased" must {

    "use the specified items" in {
      val params = buildOffsetParams()
      val result = PaginatedResult.offsetBased(items, params)
      result.items mustBe(items)
    }

    "include a first page, with offset 0" in {
      val params = buildOffsetParams(
        offset = Some(20), limit = Some(10)
      )
      val result = PaginatedResult.offsetBased(items, params)
      result.first.get.offset.get mustBe(0)
      result.first.get.limit.get mustBe(10)
    }

    "include a first page with an smaller limit, if limit is not a factor of offset" in {
      val params = buildOffsetParams(
        offset = Some(23), limit = Some(10)
      )
      val result = PaginatedResult.offsetBased(items, params)
      result.first.get.offset.get mustBe(0)
      result.first.get.limit.get mustBe(3)
    }

    "not include a previous page if currently on the first page" in {
      val params = buildOffsetParams(offset = Some(0))
      val result = PaginatedResult.offsetBased(items, params)
      result.previous mustBe (None)
    }

    "include a previous page only up to the current one" in {
      val params = buildOffsetParams(
        offset = Some(8), limit = Some(10)
      )
      val result = PaginatedResult.offsetBased(items, params)
      result.previous.get.offset.get mustBe (0)
      result.previous.get.limit.get mustBe (8)
    }

    "include a full previous page if there's room" in {
      val params = buildOffsetParams(
        offset = Some(18), limit = Some(10)
      )
      val result = PaginatedResult.offsetBased(items, params)
      result.previous.get.offset.get mustBe (8)
      result.previous.get.limit.get mustBe (10)
    }

    "include a next page" in {
      val params = buildOffsetParams(
        offset = Some(16), limit = Some(10)
      )
      val result = PaginatedResult.offsetBased(items, params)
      result.next.get.offset.get mustBe (26)
      result.next.get.limit.get mustBe (10)
    }

    "not include a next page if totalItems is specified, and is less than the limit away" in {
      val params = buildOffsetParams(limit = Some(10), offset = Some(10))
      val result = PaginatedResult.offsetBased(items, params, totalItems = Some(18))
      result.next mustBe (None)
    }

    "not include a next page if hasNext is specified false" in {
      val params = buildOffsetParams(offset = Some(20))
      val result = PaginatedResult.offsetBased(items, params, hasNext = false)
      result.next mustBe (None)
    }

    "not include a last page if totalItems is not specified" in {
      val params = buildOffsetParams(page = Some(2))
      val result = PaginatedResult.offsetBased(items, params)
      result.last mustBe (None)
    }

    "include a last page if totalItems is specified" in {
      val params = buildOffsetParams(limit = Some(10), offset = Some(20))
      val result = PaginatedResult.offsetBased(items, params, totalItems = Some(108))
      result.last.get.offset.get mustBe (100)
      result.last.get.limit.get mustBe (10)
    }

    "include non-page parameters as-is for all pages, but igoring page" in {
      val params = buildOffsetParams(
        limit = Some(10), page = Some(5), offset = Some(40), order = Some("rank"),
        config = PaginationParams.defaultPaginationConfig.copy(
          validOrders = Seq("rank", "votes"), defaultOrder = "votes"
        )
      )

      val result = PaginatedResult.offsetBased(items, params, totalItems = Some(120))
      Seq(result.first.get, result.previous.get, result.next.get, result.last.get).foreach { params =>
        params.limit.get mustBe (10)
        params.order.get mustBe ("rank")
        params.page mustBe (None)
      }
    }

  }

}
