package io.useless.play.client

import play.api.libs.ws.WSResponse

import io.useless.http.LinkHeader

object Page {

  def apply[T](items: Seq[T], response: WSResponse): Page[T] = {
    val optLinks = response.header("Link").map { rawLinks =>
      LinkHeader.parse(rawLinks)
    }

    def link(relation: String) = optLinks.flatMap { links =>
      links.find(_.relation == relation).map(_.url)
    }

    Page(items, link("first"), link("previous"), link("next"), link("last"))
  }

}

case class Page[T] (
  items: Seq[T],
  first: Option[String],
  previous: Option[String],
  next: Option[String],
  last: Option[String]
)
