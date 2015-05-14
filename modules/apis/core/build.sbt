Defaults.Settings.base

Mongo.defaultSettings

libraryDependencies ++= Seq(
  Defaults.Dependencies.reactiveMongo,
  Defaults.Dependencies.scalaTestPlay
)

javaOptions in Test += "-Dconfig.file=conf/core.test.conf"
