name := "useless"

scalaVersion in ThisBuild := "2.11.6"

lazy val lib = project.configs(IntegrationTest).settings(Defaults.itSettings: _*)

lazy val core = (project in file("apis/core")).enablePlugins(PlayScala).dependsOn(lib)

lazy val books = (project in file("apis/books")).enablePlugins(PlayScala).dependsOn(lib)
lazy val haiku = (project in file("apis/haiku")).enablePlugins(PlayScala).dependsOn(lib)

lazy val auth = (project in file("apps/auth")).enablePlugins(PlayScala).dependsOn(lib)
lazy val account = (project in file("apps/account")).enablePlugins(PlayScala).dependsOn(lib)

lazy val root = (project in file(".")).
  enablePlugins(PlayScala).
  dependsOn(core, haiku, books, auth, account).
  aggregate(lib, core, haiku, books, auth, account)

parallelExecution in Global := false

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature", "-language:reflectiveCalls")
