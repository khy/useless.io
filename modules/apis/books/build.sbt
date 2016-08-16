libraryDependencies ++= Default.DependencyGroup.postgres ++  Seq(
  ws,
  jdbc,
  "com.github.tminglei" %% "slick-pg" % "0.9.1",
  Default.Dependency.scalaTestPlay
)

routesGenerator := InjectedRoutesGenerator
