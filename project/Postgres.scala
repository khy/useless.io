import sbt._
import Keys._

object Postgres extends AutoPlugin {

  val dumpBaseDirectory = "dump/postgres"

  object autoImport {

    val postgresRemoteHost = settingKey[String](
      "The host of the remote PostgreSQL database."
    )

    val postgresRemoteUsername = settingKey[String](
      "The username used to access the remote PostgeSQL database."
    )

    val postgresRemotePassword = settingKey[String](
      "The password used to access the remote PostgreSQL database."
    )

    val postgresRemoteDatabase = settingKey[String](
      "The name of the remote PostgreSQL database to connect to."
    )

    val postgresDumpRemote = taskKey[File](
      "Dumps the configured remote PostgreSQL database, returning the dump file."
    )

  }

  import autoImport._

  override def projectSettings = Seq(

    postgresDumpRemote := {
      IO.createDirectory(file(dumpBaseDirectory))
      val dumpFile = file(dumpBaseDirectory + "/" + (System.currentTimeMillis / 1000))

      val command = Seq(
        "pg_dump",
        "--host", postgresRemoteHost.value,
        "--username", postgresRemoteUsername.value,
        "--dbname", postgresRemoteDatabase.value
      )

      Process(command, None, "PGPASSWORD" -> postgresRemotePassword.value) #> dumpFile !

      dumpFile
    }

  )

}
