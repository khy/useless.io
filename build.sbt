import com.typesafe.sbt.pgp.PgpKeys.publishSigned

name := "useless"

scalaVersion in ThisBuild := "2.11.8"

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature", "-language:reflectiveCalls")

parallelExecution in Global := false

lazy val lib = (project in file("modules/lib")).enablePlugins(Base).configs(IntegrationTest).settings(sbt.Defaults.itSettings: _*)

lazy val core = (project in file("modules/apis/core")).enablePlugins(Base, PlayScala, Mongo, Postgres, Sem).dependsOn(lib % "test->test;compile->compile")

lazy val books = (project in file("modules/apis/books")).enablePlugins(Base, PlayScala, Postgres, Sem).dependsOn(lib % "test->test;compile->compile")
lazy val budget = (project in file("modules/apis/budget")).enablePlugins(Base, PlayScala, Postgres).dependsOn(lib % "test->test;compile->compile")
lazy val haiku = (project in file("modules/apis/haiku")).enablePlugins(Base, PlayScala, Postgres, Sem).dependsOn(lib % "test->test;compile->compile")
lazy val workouts = (project in file("modules/apis/workouts")).enablePlugins(Base, PlayScala, Postgres, Sem).dependsOn(lib % "test->test;compile->compile")

lazy val auth = (project in file("modules/apps/auth")).enablePlugins(Base, PlayScala, Mongo).dependsOn(lib % "test->test;compile->compile")
lazy val account = (project in file("modules/apps/account")).enablePlugins(Base, PlayScala, Mongo).dependsOn(lib % "test->test;compile->compile")

lazy val root = (project in file(".")).
  enablePlugins(PlayScala, DockerPlugin, Release).
  dependsOn(core, books, budget, haiku, workouts, auth, account).
  aggregate(lib, core, books, budget, haiku, workouts, auth, account).
  settings(
    aggregate in stage := false,
    aggregate in publishLocal := false,
    aggregate in publish := false,
    routesGenerator := InjectedRoutesGenerator
  )

dockerBaseImage := "java:8"
maintainer in Docker := "Kevin Hyland <khy@me.com>"
dockerRepository := Some("khyland")
dockerCmd := Seq(
  "-Dconfig.file=conf/prod.conf",
  "-DapplyEvolutions.budget=true"
)

useGpg := true

publishStepTasks := Seq(
  (publishSigned in lib),
  (publish in (root, Docker)),
  (semPublish in core),
  (semPublish in books),
  (semPublish in haiku)
)

javaOptions in (ThisBuild, Test) ++= Seq(
  "-Dconfig.file=" + baseDirectory.value + "/conf/test.conf",
  "-Dlogger.file=" + baseDirectory.value + "/conf/logback-test.xml"
)
