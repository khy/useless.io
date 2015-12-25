libraryDependencies ++= Seq(
  Default.Dependency.reactiveMongo,
  Default.Dependency.jodaTime,
  "org.mindrot" % "jbcrypt" % "0.3m"
) ++ Seq (
  "org.seleniumhq.selenium" % "selenium-java" % "2.39.0" % "test"
)
