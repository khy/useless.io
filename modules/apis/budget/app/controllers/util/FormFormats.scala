package controllers.budget.util

import java.util.UUID
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.{Formatter, Formats}

// TODO: Delete me once we upgrade to Play 2.4
object FormFormats {

  import Formats._

  private def parsing[T](parse: String => T, errMsg: String, errArgs: Seq[Any])(key: String, data: Map[String, String]): Either[Seq[FormError], T] = {
    stringFormat.bind(key, data).right.flatMap { s =>
      scala.util.control.Exception.allCatch[T]
        .either(parse(s))
        .left.map(e => Seq(FormError(key, errMsg, errArgs)))
    }
  }

  implicit def uuidFormat: Formatter[UUID] = new Formatter[UUID] {
    override val format = Some(("format.uuid", Nil))
    override def bind(key: String, data: Map[String, String]) = parsing(UUID.fromString, "error.uuid", Nil)(key, data)
    override def unbind(key: String, value: UUID) = Map(key -> value.toString)
  }

  val uuid: Mapping[java.util.UUID] = of[java.util.UUID]

}
