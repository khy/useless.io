libraryDependencies ++= Default.DependencyGroup.postgres ++ Seq(
  ws,
  jdbc,
  Default.Dependency.jodaTime,
  Default.Dependency.scalaTestPlay
)
