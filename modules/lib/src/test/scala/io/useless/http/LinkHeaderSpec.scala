package io.useless.http

import java.util.UUID
import org.scalatest._
import org.scalatest.OptionValues._

class LinkHeaderSpec
  extends WordSpec
  with MustMatchers
{

  "LinkHeader.parse" must {

    "return LinkValue instances corresponding the specified Link header" in {
      val raw ="""
        <http://books.useless.io/notes?enclosure=1>; rel="enclosure",
        <http://books.useless.io/notes?hub=bub>; rel="hub",
        <http://books.useless.io/notes?subsection=a>; rel="subsection"
      """

      val linkValues = LinkHeader.parse(raw)
      linkValues.find(_.relation == "enclosure").value.url mustBe ("http://books.useless.io/notes?enclosure=1")
      linkValues.find(_.relation == "hub").value.url mustBe ("http://books.useless.io/notes?hub=bub")
      linkValues.find(_.relation == "subsection").value.url mustBe ("http://books.useless.io/notes?subsection=a")
    }

  }

  "LinkHeader.build" must {

    "return a Link header value for the specified LinkValue instances" in {
      val linkValues = Seq(
        LinkHeader.LinkValue("enclosure", "http://books.useless.io/enclosure"),
        LinkHeader.LinkValue("hub", "http://books.useless.io/hub")
      )

      val header = LinkHeader.build(linkValues)
      header.split(",").length mustBe (2)
      header must include("<http://books.useless.io/enclosure>; rel=\"enclosure\"")
      header must include("<http://books.useless.io/hub>; rel=\"hub\"")
    }

  }

}
