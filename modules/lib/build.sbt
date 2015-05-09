name := "useless"

organization := "io.useless"

version := "0.17.2"

scalacOptions ++= Seq("-deprecation")

resolvers ++= Seq(
  "Typesafe repository"     at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype OSS Snapshots"  at "https://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype OSS Releases"   at "https://oss.sonatype.org/content/groups/public"
)

val playVersion = "2.3.9"

libraryDependencies ++= Seq(
  "com.typesafe.play" %%  "play"          % playVersion,
  "com.typesafe.play" %%  "play-ws"       % playVersion,
  "com.typesafe"      %   "config"        % "1.2.1",
  "org.reactivemongo" %%  "reactivemongo" % "0.10.5.0.akka23",
  "joda-time"         %   "joda-time"     % "2.7"
) ++ Seq(
  "com.typesafe.play" %% "play-test"    % playVersion  % "test",
  "org.scalatest"     %% "scalatest"    % "2.2.4"      % "test,it",
  "org.mockito"       %  "mockito-all"  % "1.9.5"      % "test",
  "org.scalacheck"    %% "scalacheck"   % "1.12.2"     % "test"
)

parallelExecution in Test := false
