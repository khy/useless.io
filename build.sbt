import com.typesafe.sbt.pgp.PgpKeys.publishSigned

name := "useless"

scalaVersion in ThisBuild := "2.11.6"

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature", "-language:reflectiveCalls")

parallelExecution in Global := false

lazy val lib = (project in file("modules/lib")).configs(IntegrationTest).settings(sbt.Defaults.itSettings: _*)

lazy val core = (project in file("modules/apis/core")).enablePlugins(PlayScala, Mongo).dependsOn(lib)

lazy val books = (project in file("modules/apis/books")).enablePlugins(PlayScala, Postgres).dependsOn(lib)
lazy val haiku = (project in file("modules/apis/haiku")).enablePlugins(PlayScala, Mongo).dependsOn(lib)
lazy val budget = (project in file("modules/apis/budget")).enablePlugins(PlayScala, Postgres).dependsOn(lib)

lazy val auth = (project in file("modules/apps/auth")).enablePlugins(PlayScala, Mongo).dependsOn(lib)
lazy val account = (project in file("modules/apps/account")).enablePlugins(PlayScala, Mongo).dependsOn(lib)

lazy val root = (project in file(".")).
  enablePlugins(PlayScala, DockerPlugin, Release).
  dependsOn(core, books, haiku, budget, auth, account).
  aggregate(lib, core, books, haiku, budget, auth, account).
  settings(
    aggregate in stage := false,
    aggregate in publishLocal := false,
    aggregate in publish := false
  )

dockerBaseImage := "java:8"
maintainer in Docker := "Kevin Hyland <khy@me.com>"
dockerRepository := Some("khyland")
dockerCmd := Seq(
  "-Dconfig.file=conf/prod.conf",
  "-DapplyEvolutions.books=true",
  "-DapplyEvolutions.budget=true"
)

useGpg := true

publishStepTasks := Seq((publishSigned in lib), (ensureBoot2Docker), (publish in (root, Docker)))

javaOptions in (ThisBuild, Test) += "-Dconfig.file=" + baseDirectory.value + "/conf/test.conf"
