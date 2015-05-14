import sbt._
import Keys._

object Defaults {

  object Dependencies {
    val reactiveMongo = "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23"
    val jodaTime = "joda-time" %  "joda-time" % "2.2"
    val scalaTestPlay = "org.scalatestplus" %% "play" % "1.1.0" % "test"
  }

  object Settings {
    val base = Seq(
      resolvers ++= Seq(
        "Local Ivy"               at "file://" + Path.userHome.absolutePath + "/.ivy2/local",
        "Typesafe Releases"       at "http://repo.typesafe.com/typesafe/releases/",
        "Sonatype OSS Snapshots"  at "https://oss.sonatype.org/content/repositories/snapshots",
        "Sonatype OSS Releases"   at "https://oss.sonatype.org/content/groups/public"
      )
    )
  }

}
