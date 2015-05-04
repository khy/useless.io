name := "useless-haiku"

version := "1.0.2"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo" % "0.9",
  "joda-time"         %  "joda-time"     % "2.2"
)

javaOptions in Test += "-Dconfig.file=conf/haiku.test.conf"
