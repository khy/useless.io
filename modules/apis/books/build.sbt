Defaults.Settings.base

libraryDependencies ++= Defaults.DependencyGroups.postgres ++  Seq(
  ws,
  jdbc,
  "com.github.tminglei" %% "slick-pg" % "0.9.1",
  Defaults.Dependencies.scalaTestPlay
)
