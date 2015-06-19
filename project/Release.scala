import sbt._
import Keys.publish

import sbtrelease.ReleasePlugin
import ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import com.typesafe.sbt.packager.docker.DockerPlugin
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.Docker

object Release extends AutoPlugin {

  override def requires = DockerPlugin && ReleasePlugin

  object autoImport {

    val publishStepTasks = settingKey[Seq[TaskKey[_]]](
      "A sequence of sbt Tasks to be run as the 'publish' step of the release. " +
      "By default, this is simply `publish`."
    )

    val publishSteps = settingKey[Seq[ReleaseStep]](
      "A sequence of sbt-release ReleaseSteps to be run as the 'publish' step " +
      "of the release. By default, this is the `publishStepTasks`, converted to " +
      "ReleaseSteps."
    )

    val ensureBoot2Docker = taskKey[Unit](
      "Ensure that boot2docker is running."
    )

  }

  import autoImport._

  override def projectSettings = Seq(

    publishStepTasks := Seq(publish),

    publishSteps := publishStepTasks.value.map { taskKey =>
      ReleaseStep(releaseStepTask(taskKey))
    },

    ensureBoot2Docker := {
      "boot2docker up".!
    },

    releaseProcess := Seq(
      checkSnapshotDependencies,
      inquireVersions,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease
    ) ++ publishSteps.value ++ Seq(
      setNextVersion,
      commitNextVersion,
      pushChanges
    )

  )

}
