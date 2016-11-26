libraryDependencies ++= Default.DependencyGroup.postgres ++  Seq(
  jdbc,
  "com.github.tminglei" %% "slick-pg" % "0.9.1",
  "com.typesafe.play" %% "play-slick" % "1.0.1",
  Default.Dependency.scalaTestPlay
)

routesGenerator := InjectedRoutesGenerator

// testOptions in Test += Tests.Argument("-oF")
