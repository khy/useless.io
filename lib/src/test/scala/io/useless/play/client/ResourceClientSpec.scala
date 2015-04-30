package io.useless.play.client

import org.scalatest.FunSpec
import org.scalatest.Matchers
import java.util.UUID
import play.api.libs.json._

import io.useless.client._
import io.useless.account.{ Account, Api }
import io.useless.play.json.account.AccountJson._
import io.useless.test.Await
import io.useless.http.LinkHeader

class ResourceClientSpec
  extends FunSpec
  with    Matchers
{

  def resourceClient(
    status: Int,
    json: JsValue = JsNull,
    body: Option[String] = None,
    linkHeader: Option[String] = None
  ): ResourceClient = {
    val baseClient = new MockBaseClient(status, json, body, linkHeader)
    val jsonClient = new DefaultJsonClient(baseClient)
    new DefaultResourceClient(jsonClient)
  }

  describe ("ResourceClient#get") {

    it ("should throw an InvalidJsonResponseException if the baseClient returns a 200, but with invalid JSON") {
      val client = resourceClient(200, Json.obj("invalid" -> "JSON"))
      a [InvalidJsonResponseException] should be thrownBy { Await(client.get("/api")) }
    }

    it ("should return the resource if the baseClient returns a 200 with valid JSON") {
      val apiJson = Json.obj(
        "guid" -> UUID.randomUUID.toString,
        "api" -> Json.obj(
          "key" -> "museum"
        )
      )

      val client = resourceClient(200, apiJson)
      val api = Await(client.get("/api")).get.asInstanceOf[Api]
      api.key should be ("museum")
    }

  }

  describe ("ResourceClient#find") {

    it ("should throw an InvalidJsonResponseException if the baseClient returns a 200, but with invalid JSON") {
      val client = resourceClient(200, Json.obj("invalid" -> "JSON"))
      a [InvalidJsonResponseException] should be thrownBy { Await(client.find("/api")) }
    }

    it ("should throw a InvalidJsonResponseException if the baseClient returns a 200, but with a non-array") {
      val apiJson = Json.obj(
        "guid" -> UUID.randomUUID.toString,
        "api" -> Json.obj(
          "key" -> "museum"
        )
      )

      val client = resourceClient(200, apiJson)
      a [InvalidJsonResponseException] should be thrownBy { Await(client.find("/api")) }
    }

    it ("should return the sequence of resources if the baseClient returns a 200 with valid JSON") {
      val apiJson = Json.arr(Json.obj(
        "guid" -> UUID.randomUUID.toString,
        "api" -> Json.obj(
          "key" -> "museum"
        )
      ))

      val client = resourceClient(200, apiJson)
      val api = Await(client.find("/api")).items(0).asInstanceOf[Api]
      api.key should be ("museum")
    }

    it ("should return links, if included in the Link header") {
      val linkHeader = LinkHeader.build(Seq(
        LinkHeader.LinkValue("next", "http://useless.io/next")
      ))
      val json = Json.arr(Json.obj(
        "guid" -> UUID.randomUUID.toString,
        "api" -> Json.obj(
          "key" -> "museum"
        )
      ))
      val client = resourceClient(200, json, linkHeader = Some(linkHeader))
      val page = Await(client.find("/api"))
      page.next should be (Some("http://useless.io/next"))
    }

  }

  describe ("ResourceClient#create") {

    it ("should throw an InvalidJsonResponseException if the baseClient returns a 201, but with invalid JSON") {
      val client = resourceClient(201, Json.obj("invalid" -> "JSON"))
      a [InvalidJsonResponseException] should be thrownBy  { Await(client.create("/api", JsNull)) }
    }

    it ("should return the resource if the baseClient returns a 201 with valid JSON") {
      val apiJson = Json.obj(
        "guid" -> UUID.randomUUID.toString,
        "api" -> Json.obj(
          "key" -> "museum"
        )
      )

      val client = resourceClient(201, apiJson)
      Await(client.create("/api", JsNull)) match {
        case Right(api: Api) => api.key should be ("museum")
        case result => fail("Expected Right[Api], got: %s".format(result))
      }
    }

  }

}
