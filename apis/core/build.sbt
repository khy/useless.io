name := "useless-core"

version := "0.10.0"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23"
)

Mongo.defaultSettings
