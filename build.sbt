name := "useless"

scalaVersion in ThisBuild := "2.11.6"

lazy val lib = project.
  configs(IntegrationTest).
  settings(Defaults.itSettings: _*)

lazy val core = (project in file("apis/core")).enablePlugins(PlayScala).dependsOn(lib)

lazy val root = (project in file(".")).
  enablePlugins(PlayScala).
  dependsOn(core).
  aggregate(core)
