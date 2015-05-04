package io.useless.play.json.accesstoken

import org.scalatest.FunSuite
import org.scalatest.Matchers
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import play.api.libs.json.Json

import io.useless.accesstoken._
import io.useless.test.scalacheck.Arbitrary._

class AccessTokenJsonSpec
  extends FunSuite
  with    Matchers
  with    GeneratorDrivenPropertyChecks
{

  import AccessTokenJson._

  test("public AccessToken") { forAll { (accessToken: AccessToken) =>
    val accessTokenPrime = exercise(accessToken)
    accessToken.guid should be (accessTokenPrime.guid)
    accessToken.resourceOwner.guid should be (accessTokenPrime.resourceOwner.guid)
    accessToken.client.map(_.guid) should be (accessToken.client.map(_.guid))
    accessToken.scopes should be (accessTokenPrime.scopes)
  }}

  test("authorized AccessToken") { forAll { (accessToken: AuthorizedAccessToken) =>
    val accessTokenPrime = exercise(accessToken)
    accessToken.guid should be (accessTokenPrime.guid)
    accessToken.authorizationCode should be (accessTokenPrime.authorizationCode)
    accessToken.resourceOwner.guid should be (accessTokenPrime.resourceOwner.guid)
    accessToken.client.map(_.guid) should be (accessToken.client.map(_.guid))
    accessToken.scopes should be (accessTokenPrime.scopes)
  }}

  private def exercise[T <: AccessToken](account: T) = {
    val json = Json.toJson(account)
    val data = Json.stringify(json)
    val jsonPrime = Json.parse(data)
    Json.fromJson(jsonPrime).get.asInstanceOf[T]
  }

}
