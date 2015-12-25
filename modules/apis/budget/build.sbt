libraryDependencies ++= Defaults.DependencyGroups.postgres ++ Seq(
  ws,
  jdbc,
  Defaults.Dependencies.jodaTime,
  Defaults.Dependencies.scalaTestPlay
)
