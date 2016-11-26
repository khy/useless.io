package db.workouts

import slick.ast.TypedType
import Driver.api.Table

trait SchemaData[T] {

  self: Table[T] =>

  def schemaVersionMajor(implicit tt: TypedType[Int]) = column[Int]("schema_version_major")
  def schemaVersionMinor(implicit tt: TypedType[Int]) = column[Int]("schema_version_minor")

}
