name := "useless"

scalaVersion in ThisBuild := "2.11.6"

lazy val lib = project.
  configs( IntegrationTest ).
  settings( Defaults.itSettings : _*)
