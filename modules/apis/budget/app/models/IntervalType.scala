package models.budget

import io.useless.{NamedEnum, NamedEnumCompanion}

sealed class IntervalType(
  val key: String,
  val name: String
) extends NamedEnum

object IntervalType extends NamedEnumCompanion[IntervalType] {
  case object Milliseconds extends IntervalType("milliseconds", "Milliseconds")
  case object Second extends IntervalType("second", "Second")
  case object Minute extends IntervalType("minute", "Minute")
  case object Hour extends IntervalType("hour", "Hour")
  case object Day extends IntervalType("day", "Day")
  case object Week extends IntervalType("week", "Week")
  case object Month extends IntervalType("month", "Month")
  case object Quarter extends IntervalType("quarter", "Quarter")
  case object Year extends IntervalType("year", "Year")
  case object Decade extends IntervalType("decade", "Decade")
  case object Century extends IntervalType("century", "Century")
  case object Millennium extends IntervalType("millennium", "Millennium")
  case class Unknown(override val key: String) extends IntervalType(key, "Unknown")

  val values = Seq(Milliseconds, Second, Minute, Hour, Day, Week, Month, Quarter, Year, Decade, Century, Millennium)
  def unknown(key: String) = Unknown(key)
}
