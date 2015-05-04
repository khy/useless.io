package io.useless.http

object UrlUtil {

  def createQueryString(params: Map[String, Seq[String]]) = {
    params.toSeq.flatMap { case (key, values) =>
      values.map { value => key + "=" + value }
    }.mkString("&")
  }

  def appendQueryString(base: String, queryString: String) = {
    if (base.contains("?")) base + "&" + queryString
    else base + "?" + queryString
  }

}
