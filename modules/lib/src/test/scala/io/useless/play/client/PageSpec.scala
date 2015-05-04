package io.useless.play.client

import org.scalatest.{ WordSpec, MustMatchers }
import org.mockito.Mockito._
import play.api.libs.ws.WSResponse

import io.useless.http.LinkHeader

class PageSpec
  extends WordSpec
  with MustMatchers
{

  "Page.apply" should {

    "return no links if the response does not include a Link header" in {
      val response = mockWsResponse(None)
      val page = Page(Seq(1,2,3), response)

      page.items mustBe Seq(1,2,3)
      page.first mustBe None
      page.previous mustBe None
      page.next mustBe None
      page.last mustBe None
    }

    "include a partial set of links, if specified in the Link header" in {
      val linkHeader = LinkHeader.build(Seq(
        LinkHeader.LinkValue("previous", "http://useless.io/previous"),
        LinkHeader.LinkValue("next", "http://useless.io/next")
      ))
      val response = mockWsResponse(Some(linkHeader))
      val page = Page(Seq(1,2,3), response)

      page.items mustBe Seq(1,2,3)
      page.first mustBe None
      page.previous mustBe Some("http://useless.io/previous")
      page.next mustBe Some("http://useless.io/next")
      page.last mustBe None
    }

    "include a full set of links, if specified in the Link header" in {
      val linkHeader = LinkHeader.build(Seq(
        LinkHeader.LinkValue("first", "http://useless.io/first"),
        LinkHeader.LinkValue("previous", "http://useless.io/previous"),
        LinkHeader.LinkValue("next", "http://useless.io/next"),
        LinkHeader.LinkValue("last", "http://useless.io/last")
      ))
      val response = mockWsResponse(Some(linkHeader))
      val page = Page(Seq(1,2,3), response)

      page.items mustBe Seq(1,2,3)
      page.first mustBe Some("http://useless.io/first")
      page.previous mustBe Some("http://useless.io/previous")
      page.next mustBe Some("http://useless.io/next")
      page.last mustBe Some("http://useless.io/last")
    }

    def mockWsResponse(linkHeader: Option[String]) = {
      val mockWsResponse = mock(classOf[WSResponse])
      when(mockWsResponse.header("Link")).thenReturn(linkHeader)
      mockWsResponse
    }

  }

}
