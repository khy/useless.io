package io.useless.http

object LinkHeader {

  case class LinkValue(relation: String, url: String)

  val linkValueRx = """<([^>]+)>\s*;\s*rel=\"([a-z\-]+)\"""".r

  def parse(raw: String): Seq[LinkValue] = {
    linkValueRx.findAllMatchIn(raw).toSeq.map { _match =>
      LinkValue(_match.group(2), _match.group(1))
    }
  }

  def build(linkValues: Seq[LinkValue]) = linkValues.map { linkValue =>
    "<" + linkValue.url + ">; rel=\"" + linkValue.relation + "\""
  }.mkString(",")

}
