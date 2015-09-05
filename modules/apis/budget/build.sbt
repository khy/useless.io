Defaults.Settings.base

libraryDependencies ++= Defaults.DependencyGroups.postgres ++ Seq(
  ws,
  jdbc,
  Defaults.Dependencies.jodaTime,
  Defaults.Dependencies.scalaTestPlay
)

javaOptions in Test += "-Dconfig.file=conf/budget.test.conf"
