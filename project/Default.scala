import sbt._
import Keys._

object Default {

  val playVersion = "2.4.8"

  object Dependency {
    val reactiveMongo = "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23"
    val slick = "com.typesafe.slick"  %% "slick" % "3.0.2"
    val postgres = "org.postgresql" %  "postgresql" % "9.3-1102-jdbc4"
    val hikariCp = "com.zaxxer" % "HikariCP" % "2.4.1"
    val jodaTime = "joda-time" %  "joda-time" % "2.2"
    val scalaTestPlay = "org.scalatestplus" %% "play" % "1.4.0-M3" % Test
  }

  object DependencyGroup {
    val postgres = Seq(Dependency.slick, Dependency.postgres, Dependency.hikariCp)
  }

}
