import sbt._
import Keys.publish

import sbtrelease.ReleasePlugin
import sbtrelease.ReleaseStateTransformations._
import com.typesafe.sbt.packager.docker.DockerPlugin
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.Docker

object Release extends AutoPlugin {

  override def requires = DockerPlugin && ReleasePlugin

  override def projectSettings = Seq(

    ReleasePlugin.autoImport.releaseProcess := Seq(
      checkSnapshotDependencies,
      inquireVersions,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      ReleasePlugin.autoImport.releaseStepTask(publish in Docker),
      setNextVersion,
      commitNextVersion,
      pushChanges
    )

  )

}
