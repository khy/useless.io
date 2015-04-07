package models.core.account

import io.useless.accesstoken.{ Scope => UselessScope }

/*
 * An access token has zero or more scopes, which are potentially used to
 * authorize requests made using that access token.
 */
object Scope {

  lazy val internal = Seq(Platform, Auth)

  lazy val core = Seq(Platform, Auth, Admin, Trusted)

  /*
   * Allows administrative actions at the platform level, such as creating an
   * App or an Api.
   */
  val Platform = UselessScope("platform")

  /*
   * Required for an App to implement OAuth functionality, e.g. useless-auth.
   */
  val Auth = UselessScope("auth")

  /*
   * Allows administrative actions at the account level, such as listing and
   * creating new access tokens.
   */
  val Admin = UselessScope("admin")

  /*
   * Allows an Account to create access tokens without using OAuth.
   */
  val Trusted = UselessScope("trusted")

}
