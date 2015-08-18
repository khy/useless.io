Defaults.Settings.base

libraryDependencies ++= Seq(
  ws,
  jdbc,
  Defaults.Dependencies.jodaTime,
  "org.postgresql"      %  "postgresql" % "9.3-1102-jdbc4",
  "com.typesafe.slick"  %% "slick"      % "2.1.0",
  Defaults.Dependencies.scalaTestPlay
)

javaOptions in Test += "-Dconfig.file=conf/moofin.test.conf"
