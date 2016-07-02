package io.useless.play.http

import java.util.UUID
import scala.util.control.Exception._
import play.api.mvc.Request
import org.joda.time.DateTime

object QueryStringUtil {

  implicit class RichQueryStringRequest(request: Request[_]) {
    val laxQueryString = new LaxQueryString(request)
  }

}

class LaxQueryString(request: Request[_]) {
  def seq[T](key: String, delim: Option[String] = None)(parse: String => Option[T]) = LaxQueryString.seq(request, key, delim)(parse)
  def seqString(key: String, delim: Option[String] = None) = LaxQueryString.seqString(request, key, delim)
  def string(key: String) = LaxQueryString.seqString(request, key).flatMap(_.headOption)
  def seqInt(key: String, delim: Option[String] = None) = LaxQueryString.seqInt(request, key, delim)
  def int(key: String) = LaxQueryString.seqInt(request, key).flatMap(_.headOption)
  def seqLong(key: String, delim: Option[String] = None) = LaxQueryString.seqLong(request, key, delim)
  def long(key: String) = LaxQueryString.seqLong(request, key).flatMap(_.headOption)
  def seqUuid(key: String, delim: Option[String] = None) = LaxQueryString.seqUuid(request, key, delim)
  def uuid(key: String) = LaxQueryString.seqUuid(request, key).flatMap(_.headOption)
  def seqDateTime(key: String, delim: Option[String] = None) = LaxQueryString.seqDateTime(request, key, delim)
  def dateTime(key: String) = LaxQueryString.seqDateTime(request, key).flatMap(_.headOption)
  def boolean(key: String) = LaxQueryString.boolean(request, key)
}

object LaxQueryString {

  def seq[T](request: Request[_], key: String, delim: Option[String] = None)(parse: String => Option[T]): Option[Seq[T]] = {
    request.queryString.get(key).map { raws =>
      raws.flatMap { raw =>
        delim.map { delim => raw.split(delim).toSeq }.getOrElse(Seq(raw))
      }.map(parse).filter(_.isDefined).map(_.get)
    }
  }

  def seqString(request: Request[_], key: String, delim: Option[String] = None): Option[Seq[String]] = {
    seq(request, key, delim) { Some(_) }
  }

  def seqInt(request: Request[_], key: String, delim: Option[String] = None): Option[Seq[Int]] = {
    seq(request, key, delim) { rawInt =>
      catching(classOf[NumberFormatException]) opt rawInt.toInt
    }
  }

  def seqLong(request: Request[_], key: String, delim: Option[String] = None): Option[Seq[Long]] = {
    seq(request, key, delim) { rawLong =>
      catching(classOf[NumberFormatException]) opt rawLong.toLong
    }
  }

  def seqUuid(request: Request[_], key: String, delim: Option[String] = None): Option[Seq[UUID]] = {
    seq(request, key, delim) { rawUuid =>
      catching(classOf[IllegalArgumentException]) opt UUID.fromString(rawUuid)
    }
  }

  def seqDateTime(request: Request[_], key: String, delim: Option[String] = None): Option[Seq[DateTime]] = {
    seq(request, key, delim) { rawDateTime =>
      catching(classOf[IllegalArgumentException]) opt DateTime.parse(rawDateTime)
    }
  }

  def boolean(request: Request[_], key: String): Option[Boolean] = {
    seq(request, key, None) {
      case "true" => Some(true)
      case "false" => Some(false)
      case _ => None
    }.flatMap(_.headOption)
  }

}
