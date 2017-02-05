package io.useless.account

import java.util.UUID

import io.useless.util.Validator

trait User extends Account {

  def handle: String

  def name: Option[String]

  require(Validator.isValidHandle(handle), "handle is invalid")

}

class PublicUser(
  val guid: UUID,
  val handle: String,
  val name: Option[String]
) extends User

class AuthorizedUser(
  val guid: UUID,
  val email: String,
  val handle: String,
  val name: Option[String]
) extends User {

  require(Validator.isValidEmail(email), "email is invalid")

}

object User {

  def apply(
    guid: UUID,
    handle: String,
    name: Option[String]
  ): User = {
    User.public(guid, handle, name)
  }

  def public(
    guid: UUID,
    handle: String,
    name: Option[String]
  ): PublicUser = {
    new PublicUser(guid, handle, name)
  }

  def authorized(
    guid: UUID,
    email: String,
    handle: String,
    name: Option[String]
  ): AuthorizedUser = {
    new AuthorizedUser(guid, email, handle, name)
  }

  val Anon: User = new PublicUser(
    guid = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    handle = "anon",
    name = Some("Anon E. Muss")
  )

}
