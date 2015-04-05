package io.useless.play.json

import org.scalatest.FunSuite
import org.scalatest.Matchers
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import play.api.libs.json.Json

import io.useless.ClientError
import io.useless.test.scalacheck.Arbitrary._

class ClientErrorJsonSpec
  extends FunSuite
  with    Matchers
  with    GeneratorDrivenPropertyChecks
{

  import ClientErrorJson._

  test("ClientError") { forAll { (clientError: ClientError) =>
    val json = Json.toJson(clientError)
    val data = Json.stringify(json)
    val jsonPrime = Json.parse(data)
    val clientErrorPrime = Json.fromJson(jsonPrime).get.asInstanceOf[ClientError]

    clientError.key should be (clientErrorPrime.key)
    clientError.details should be (clientErrorPrime.details)
  }}

}
