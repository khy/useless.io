libraryDependencies ++= Default.DependencyGroup.postgres ++ Seq(
  Default.Dependency.jodaTime,
  "org.scalatestplus" %% "play" % "1.1.0" % "test"
)
