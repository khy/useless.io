name := "useless"

scalaVersion in ThisBuild := "2.11.6"

lazy val lib = project.
  configs(IntegrationTest).
  settings(Defaults.itSettings: _*)

lazy val core = (project in file("apis/core")).enablePlugins(PlayScala).dependsOn(lib)
lazy val haiku = (project in file("apis/haiku")).enablePlugins(PlayScala).dependsOn(lib)

lazy val root = (project in file(".")).
  enablePlugins(PlayScala).
  dependsOn(core, haiku).
  aggregate(lib, core, haiku)

parallelExecution in Global := false
