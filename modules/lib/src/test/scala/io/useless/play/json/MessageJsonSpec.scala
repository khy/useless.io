package io.useless.play.json

import org.scalatest.FunSuite
import org.scalatest.Matchers
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import play.api.libs.json.Json

import io.useless.Message
import io.useless.test.scalacheck.Arbitrary._

class MessageJsonSpec
  extends FunSuite
  with    Matchers
  with    GeneratorDrivenPropertyChecks
{

  import MessageJson._

  test("Message") { forAll { (clientError: Message) =>
    val json = Json.toJson(clientError)
    val data = Json.stringify(json)
    val jsonPrime = Json.parse(data)
    val clientErrorPrime = Json.fromJson(jsonPrime).get.asInstanceOf[Message]

    clientError.key should be (clientErrorPrime.key)
    clientError.details should be (clientErrorPrime.details)
  }}

}
