package io.useless.play.client

import java.util.UUID
import scala.concurrent.Future
import org.mockito.Mockito._
import play.api.libs.json.{ JsValue, JsNull }
import play.api.libs.ws.WSResponse

trait MockBaseClientComponent extends BaseClientComponent {

  class MockBaseClient(
    status: Int,
    json: JsValue = JsNull,
    body: Option[String] = None,
    linkHeader: Option[String] = None
  ) extends BaseClient {

    val auth = UUID.randomUUID.toString

    def get(path: String, query: (String, String)*) = _mock

    def post(path: String, body: JsValue) = _mock

    private lazy val _mock = {
      val mockResponse = mock(classOf[WSResponse])
      when(mockResponse.status).thenReturn(status)
      when(mockResponse.json).thenReturn(json)
      when(mockResponse.body).thenReturn(body.getOrElse(json.toString))
      when(mockResponse.header("Link")).thenReturn(linkHeader)
      Future.successful(mockResponse)
    }

  }

}
