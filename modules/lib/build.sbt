organization := "io.useless"

val playVersion = "2.3.9"

libraryDependencies ++= Seq(
  Default.Dependency.reactiveMongo,
  Default.Dependency.jodaTime,
  "com.typesafe.play" %%  "play"          % playVersion,
  "com.typesafe.play" %%  "play-ws"       % playVersion,
  "com.typesafe"      %   "config"        % "1.2.1"
) ++ Seq(
  Default.Dependency.scalaTestPlay,
  "com.typesafe.play" %% "play-test"    % playVersion  % "test",
  "org.scalatest"     %% "scalatest"    % "2.2.4"      % "test,it",
  "org.mockito"       %  "mockito-all"  % "1.9.5"      % "test",
  "org.scalacheck"    %% "scalacheck"   % "1.12.2"     % "test"
)
