name := "useless-books"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  ws,
  jdbc,
  "org.postgresql"      %  "postgresql" % "9.3-1102-jdbc4",
  "com.typesafe.slick"  %% "slick"      % "2.1.0",
  "com.github.tminglei" %% "slick-pg"   % "0.6.3",
  "org.scalatestplus"   %% "play"       % "1.1.0"           % "test"
)

javaOptions in Test += "-Dconfig.file=conf/books.test.conf"
