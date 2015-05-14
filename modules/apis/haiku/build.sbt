Defaults.Settings.base

libraryDependencies ++= Seq(
  Defaults.Dependencies.reactiveMongo,
  Defaults.Dependencies.jodaTime
)

javaOptions in Test += "-Dconfig.file=conf/haiku.test.conf"
