publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>http://useless.io</url>
  <licenses>
    <license>
      <name>BSD-style</name>
      <url>http://www.opensource.org/licenses/bsd-license.php</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:useless-io/useless.scala.git</url>
    <connection>scm:git:git@github.com:useless-io/useless.scala.git</connection>
  </scm>
  <developers>
    <developer>
      <id>khy</id>
      <name>Kevin Hyland</name>
      <url>https://github.com/khy</url>
    </developer>
  </developers>
)
