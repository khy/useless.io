import sbt._
import Keys._

object Sem extends AutoPlugin {

  object autoImport {

    val semSchemaDir = settingKey[String](
      "The base sem directory. Defaults to 'schema'."
    )

    val semArtifactName = settingKey[String](
      "The base name of the sem artifact. Defaults to 'schema-[name]'."
    )

    val semTag = settingKey[String](
      "The tag used to name the sem artifact. Defaults to the current 'v[version]''."
    )

    val semDist = taskKey[File](
      "Generates the sem artifact for the current version."
    )

    val semS3PublishFolder = settingKey[String](
      "The S3 folder to publish the sem dist to. Defaults to 'evenfinancial/sem/[name]/'"
    )

    val semPublish = taskKey[Unit](
      "Publishes the sem artifact of the current version to S3."
    )

  }

  import autoImport._

  override def projectSettings = Seq(

    semSchemaDir := baseDirectory.value + "/schema",

    semArtifactName := "schema-" + name.value,

    semTag := "v" + version.value,

    semDist := {
      Process(
        command = Seq("sem-dist",
          "--artifact_name", semArtifactName.value,
          "--tag", semTag.value
        ),
        cwd = file(semSchemaDir.value)
      ).!

      file(semSchemaDir.value + "/dist/" + semArtifactName.value + "-" + semTag.value + ".tar.gz")
    },

    semS3PublishFolder := "useless.io/sem/" + name.value + "/",

    semPublish := {
      Seq(
        "aws", "--profile", "khy",
        "s3", "cp",
        semDist.value.getPath, "s3://" + semS3PublishFolder.value
      ).!
    }
  )
}
