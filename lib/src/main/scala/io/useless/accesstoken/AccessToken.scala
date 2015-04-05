package io.useless.accesstoken

import java.util.UUID

import io.useless.account.Account

/**
 * AccessToken defines how requests are authenticated. Every authenticated
 * request must specify an AccessToken guid, e.g. via the Authorization header.
 *
 * An AccessToken MIGHT have a client - this is the account that is actually
 * making the request. If this is missing, then the account is making a request
 * as itself.
 *
 * An AccessToken also has zero or more Scopes, upon which the authenticating
 * API can implement authorization.
 */
trait AccessToken {

  def guid: UUID

  def resourceOwner: Account

  def client: Option[Account]

  def scopes: Seq[Scope]

}

class PublicAccessToken(
  val guid: UUID,
  val resourceOwner: Account,
  val client: Option[Account],
  val scopes: Seq[Scope]
) extends AccessToken

class AuthorizedAccessToken(
  val guid: UUID,
  val authorizationCode: UUID,
  val resourceOwner: Account,
  val client: Option[Account],
  val scopes: Seq[Scope]
) extends AccessToken

object AccessToken {

  def apply(
    guid: UUID,
    resourceOwner: Account,
    client: Option[Account],
    scopes: Seq[Scope]
  ): AccessToken = {
    AccessToken.public(guid, resourceOwner, client, scopes)
  }

  def public(
    guid: UUID,
    resourceOwner: Account,
    client: Option[Account],
    scopes: Seq[Scope]
  ): PublicAccessToken = {
    new PublicAccessToken(guid, resourceOwner, client, scopes)
  }

  def authorized(
    guid: UUID,
    authorizationCode: UUID,
    resourceOwner: Account,
    client: Option[Account],
    scopes: Seq[Scope]
  ): AuthorizedAccessToken = {
    new AuthorizedAccessToken(guid, authorizationCode, resourceOwner, client, scopes)
  }

}
