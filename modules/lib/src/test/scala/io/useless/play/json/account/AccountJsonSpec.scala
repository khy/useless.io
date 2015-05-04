package io.useless.play.json.account

import org.scalatest.FunSuite
import org.scalatest.Matchers
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import play.api.libs.json.Json

import io.useless.account._
import io.useless.test.scalacheck.Arbitrary._

class AccountJsonSpec
  extends FunSuite
  with    Matchers
  with    GeneratorDrivenPropertyChecks
{

  import AccountJson._

  test("public Api") { forAll { (api: Api) =>
    val apiPrime = exercise(api)
    api.guid should be (apiPrime.guid)
    api.key should be (apiPrime.key)
  }}

  test("public App") { forAll { (app: App) =>
    val appPrime = exercise(app)
    app.guid should be (appPrime.guid)
    app.name should be (appPrime.name)
    app.url should be (appPrime.url)
  }}

  test("authorized App") { forAll { (app: AuthorizedApp) =>
    val appPrime = exercise(app)
    app.guid should be (appPrime.guid)
    app.name should be (appPrime.name)
    app.url should be (appPrime.url)
    app.authRedirectUrl should be (appPrime.authRedirectUrl)
  }}

  test("public User") { forAll { (user: User) =>
    val userPrime = exercise(user)
    user.guid should be (userPrime.guid)
    user.handle should be (userPrime.handle)
    user.name should be (userPrime.name)
  }}

  test("authorized User") { forAll { (user: AuthorizedUser) =>
    val userPrime = exercise(user)
    user.guid should be (userPrime.guid)
    user.email should be (userPrime.email)
    user.handle should be (userPrime.handle)
    user.name should be (userPrime.name)
  }}

  private def exercise[T <: Account](account: T) = {
    val json = Json.toJson(account)
    val data = Json.stringify(json)
    val jsonPrime = Json.parse(data)
    Json.fromJson(jsonPrime).get.asInstanceOf[T]
  }

}
