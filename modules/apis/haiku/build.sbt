Defaults.Settings.base

libraryDependencies ++= Seq(
  Defaults.Dependencies.reactiveMongo,
  Defaults.Dependencies.jodaTime,
  "org.scalaz" %% "scalaz-core" % "7.1.3",
  "org.scalatestplus" %% "play" % "1.1.0" % "test"
)

javaOptions in Test += "-Dconfig.file=conf/haiku.test.conf"
