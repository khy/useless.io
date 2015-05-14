name := "useless"

scalaVersion in ThisBuild := "2.11.6"

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature", "-language:reflectiveCalls")

parallelExecution in Global := false

lazy val lib = (project in file("modules/lib")).configs(IntegrationTest).settings(sbt.Defaults.itSettings: _*)

lazy val core = (project in file("modules/apis/core")).enablePlugins(PlayScala).dependsOn(lib)

lazy val books = (project in file("modules/apis/books")).enablePlugins(PlayScala, Postgres).dependsOn(lib)
lazy val haiku = (project in file("modules/apis/haiku")).enablePlugins(PlayScala).dependsOn(lib)

lazy val auth = (project in file("modules/apps/auth")).enablePlugins(PlayScala).dependsOn(lib)
lazy val account = (project in file("modules/apps/account")).enablePlugins(PlayScala).dependsOn(lib)

lazy val root = (project in file(".")).
  enablePlugins(PlayScala).
  dependsOn(core, haiku, books, auth, account).
  aggregate(lib, core, haiku, books, auth, account)
