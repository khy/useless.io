import sbt._
import Keys._

object Base extends AutoPlugin {

  override def projectSettings = Seq(

    resolvers ++= Seq(
      "Local Ivy"               at "file://" + Path.userHome.absolutePath + "/.ivy2/local",
      "Typesafe Releases"       at "http://repo.typesafe.com/typesafe/releases/",
      "Sonatype OSS Snapshots"  at "https://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype OSS Releases"   at "https://oss.sonatype.org/content/groups/public"
    )

  )

}
