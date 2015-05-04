package models.haiku.mongo

import java.util.UUID
import reactivemongo.bson._
import org.joda.time.DateTime
import io.useless.reactivemongo.bson.UuidBson._
import io.useless.reactivemongo.bson.DateTimeBson._

object HaikuMongo {

  class HaikuDocument(
    val guid: UUID,
    val lines: Seq[String],
    val createdByGuid: UUID,
    val createdAt: DateTime
  )

  implicit object HaikuBSONReader extends BSONDocumentReader[HaikuDocument] {
    def read(haiku: BSONDocument): HaikuDocument = {
      new HaikuDocument(
        haiku.getAs[UUID]("_id").get,
        haiku.getAs[Seq[String]]("lines").get,
        haiku.getAs[UUID]("created_by_guid").get,
        haiku.getAs[DateTime]("created_at").get
      )
    }
  }

  implicit object HaikuBSONWriter extends BSONDocumentWriter[HaikuDocument] {
    def write(haiku: HaikuDocument): BSONDocument = {
      BSONDocument(
        "_id" -> haiku.guid,
        "lines" -> haiku.lines,
        "created_by_guid" -> haiku.createdByGuid,
        "created_at" -> haiku.createdAt
      )
    }
  }

}
