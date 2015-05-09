name := "useless-auth"

version := "0.2.2"

libraryDependencies ++= Seq(
  "org.reactivemongo" %%  "reactivemongo" % "0.10.5.0.akka23",
  "joda-time"         %   "joda-time"     % "2.2",
  "org.mindrot"       %   "jbcrypt"       % "0.3m"
) ++ Seq (
  "org.seleniumhq.selenium" % "selenium-java" % "2.39.0" % "test"
)

javaOptions in Test += "-Dconfig.file=conf/auth.test.conf"
