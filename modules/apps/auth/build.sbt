Defaults.Settings.base

libraryDependencies ++= Seq(
  Defaults.Dependencies.reactiveMongo,
  Defaults.Dependencies.jodaTime,
  "org.mindrot" % "jbcrypt" % "0.3m"
) ++ Seq (
  "org.seleniumhq.selenium" % "selenium-java" % "2.39.0" % "test"
)

javaOptions in Test += "-Dconfig.file=conf/auth.test.conf"
