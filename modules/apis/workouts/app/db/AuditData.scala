package db.workouts

import java.util.UUID
import java.time.ZonedDateTime

import slick.ast.TypedType
import Driver.api.Table

trait AuditData[T] {

  self: Table[T] =>

  def createdAt(implicit tt: TypedType[ZonedDateTime]) = column[ZonedDateTime]("created_at")
  def createdByAccount(implicit tt: TypedType[UUID])  = column[UUID]("created_by_account")
  def createdByAccessToken(implicit tt: TypedType[UUID]) = column[UUID]("created_by_access_token")
  def deletedAt(implicit tt: TypedType[ZonedDateTime])  = column[Option[ZonedDateTime]]("deleted_at")
  def deletedByAccount(implicit tt: TypedType[UUID]) = column[Option[UUID]]("deleted_by_account")
  def deletedByAccessToken(implicit tt: TypedType[UUID]) = column[Option[UUID]]("deleted_by_access_token")

}
