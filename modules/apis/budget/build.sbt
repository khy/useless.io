Defaults.Settings.base

libraryDependencies ++= Seq(
  ws,
  jdbc,
  Defaults.Dependencies.jodaTime,
  "org.postgresql"      %  "postgresql" % "9.3-1102-jdbc4",
  "com.typesafe.slick"  %% "slick"      % "3.0.2",
  "com.zaxxer"          %  "HikariCP"   % "2.3.5",
  Defaults.Dependencies.scalaTestPlay
)

javaOptions in Test += "-Dconfig.file=conf/budget.test.conf"
