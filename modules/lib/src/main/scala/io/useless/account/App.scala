package io.useless.account

import java.util.UUID

trait App extends Account {

  def name: String

  def url: String

}

class PublicApp(
  val guid: UUID,
  val name: String,
  val url: String
) extends App

class AuthorizedApp(
  val guid: UUID,
  val name: String,
  val url: String,
  val authRedirectUrl: String
) extends App

object App {

  def apply(
    guid: UUID,
    name: String,
    url: String
  ): App = {
    App.public(guid, name, url)
  }

  def public(
    guid: UUID,
    name: String,
    url: String
  ): PublicApp = {
    new PublicApp(guid, name, url)
  }

  def authorized(
    guid: UUID,
    name: String,
    url: String,
    authRedirectUrl: String
  ): AuthorizedApp = {
    new AuthorizedApp(guid, name, url, authRedirectUrl)
  }

}
