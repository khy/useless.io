package io.useless.play.http

import java.util.UUID
import scala.util.control.Exception._
import play.api.mvc.Request
import org.joda.time.DateTime

import io.useless.typeclass.Parse
import io.useless.validation.{Validation, Validator}

object QueryStringUtil {

  implicit class RichQueryStringRequest(request: Request[_]) {
    val laxQueryString = new LaxQueryString(request)
  }

  implicit val stringParse = new Parse[String] {
    def parse(raw: String) = Validation.success(raw)
  }

  implicit val intParse = new Parse[Int] {
    def parse(raw: String) = Validator.int(raw)
  }

  implicit val longParse = new Parse[Long] {
    def parse(raw: String) = Validator.long(raw)
  }

  implicit val uuidParse = new Parse[UUID] {
    def parse(raw: String) = Validator.uuid(raw)
  }

  implicit val dateTimeParse = new Parse[DateTime] {
    def parse(raw: String) = Validator.dateTime(raw)
  }

  implicit val booleanParse = new Parse[Boolean] {
    def parse(raw: String) = Validator.boolean(raw)
  }

}

class LaxQueryString(request: Request[_]) {

  def seq[T: Parse](key: String, delim: Option[String] = None): Option[Seq[T]] = {
    request.queryString.get(key).map { raws =>
      raws.flatMap { raw =>
        delim.map { delim => raw.split(delim).toSeq }.getOrElse(Seq(raw))
      }.map { raw =>
        implicitly[Parse[T]].parse(raw).toOption
      }.filter(_.isDefined).map(_.get)
    }
  }

  def get[T: Parse](key: String): Option[T] = seq(key).flatMap(_.headOption)

}
