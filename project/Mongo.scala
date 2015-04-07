import sbt._
import Keys._
import com.typesafe.sbt.packager.Keys.stage

object Mongo {

  val productionDumpDir = "dump/production"

  val devDatabase = "useless_core_dev"

  object Keys {

    val productionMongoHost = settingKey[String](
      "The host of the production Mongo DB."
    )

    val productionMongoPort = settingKey[Int](
      "The port of the production Mongo DB."
    )

    val productionMongoDatabase = settingKey[String](
      "The name of the production Mongo DB "
    )

    val productionMongoUsername = settingKey[String](
      "The username used to access the production Mongo DB."
    )

    val productionMongoPassword = settingKey[String](
      "The password used to access the production Mongo DB"
    )

    val dumpProductionMongo = taskKey[Unit](
      "Dumps the production Mongo DB to a unique subdirectory of the " +
      s"'${productionDumpDir}' directory"
    )

    val restoreLastProductionMongoDump = taskKey[Unit](
      s"Restores the latest Mongo dump in the '${productionDumpDir}' directory."
    )

    val restoreMongoFromProduction = taskKey[Unit](
      "Dumps the production Mongo DB and restores it."
    )

  }

  import Keys._

  val defaultSettings = Seq(

    // Dummy values that should be overriden somewhere.
    productionMongoHost := "localhost",

    productionMongoPort := 27017,

    productionMongoDatabase := "dummy",

    productionMongoUsername := "dummy",

    productionMongoPassword := "secret",

    dumpProductionMongo := {
      val host = productionMongoHost.value
      val port = productionMongoPort.value
      val database = productionMongoDatabase.value
      val username = productionMongoUsername.value
      val password = productionMongoPassword.value
      val subDirectory = System.currentTimeMillis / 1000

      List("mongodump",
        "-h", s"${host}:${port}",
        "-d", database,
        "-u", username,
        "-p", password,
        "-o", s"${productionDumpDir}/${subDirectory}"
      ).!
    },

    restoreLastProductionMongoDump := {
      s"mkdir -p ${productionDumpDir}".!
      val cmd = s"ls ${productionDumpDir}" #| "sort -r" #| "head -n 1"
      val latestDumpSubDirectory = cmd.lines_!.headOption

      latestDumpSubDirectory.map { subDirectory =>
        val database = productionMongoDatabase.value

        List("mongo", devDatabase, "--eval", "db.dropDatabase()").!

        List(
          "mongorestore",
          "--db", devDatabase,
          s"${productionDumpDir}/${subDirectory}/${database}"
        ).!
      }.getOrElse {
        throw new RuntimeException(s"No dumps in '${productionDumpDir}'")
      }
    },

    restoreMongoFromProduction <<= restoreLastProductionMongoDump.dependsOn(dumpProductionMongo)

  )

}
