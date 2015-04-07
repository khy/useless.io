name := "useless-core"

version := "0.10.0"

resolvers ++= Seq(
  "Local Ivy"               at "file://" + Path.userHome.absolutePath + "/.ivy2/local",
  "Sonatype OSS Snapshots"  at "https://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype OSS Releases"   at "https://oss.sonatype.org/content/groups/public"
)

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23"
)

javaOptions in Test += "-Dconfig.file=conf/test.conf"

Mongo.defaultSettings
