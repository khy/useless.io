package support

import java.util.UUID
import scala.concurrent.Future
import play.api.Application
import play.api.test.FakeApplication
import play.api.libs.json.JsValue
import play.api.libs.ws.{ WS, WSResponse }
import play.api.test.Helpers._

trait RequestHelpers {

  val appWithRoute = {
    FakeApplication(additionalConfiguration = Map("application.router" -> "core.Routes"))
  }

  def block[T](future: Future[T]) = await(future)

  def post(url: String, auth: UUID, body: JsValue)(implicit app: Application): WSResponse =
    post(url, auth.toString, body)

  def post(url: String, auth: String, body: JsValue)(implicit app: Application): WSResponse =
    post(url, Some(auth), body)

  def post(url: String, auth: Option[String], body: JsValue)(implicit app: Application): WSResponse = {
    var request = WS.url(url)

    auth.foreach { auth =>
      request = request.withHeaders(("Authorization" -> auth))
    }

    await { request.post(body) }
  }

  def put(url: String, auth: UUID, body: JsValue)(implicit app: Application): WSResponse =
    put(url, Some(auth.toString), body)

  def put(url: String, auth: Option[String], body: JsValue)(implicit app: Application): WSResponse = {
    var request = WS.url(url)

    auth.foreach { auth =>
      request = request.withHeaders(("Authorization" -> auth))
    }

    await { request.put(body) }
  }

  def get(url: String, auth: UUID, query: (String, String)*)(implicit app: Application): WSResponse =
    get(url, auth.toString, query:_*)

  def get(url: String, auth: String, query: (String, String)*)(implicit app: Application): WSResponse =
    get(url, Some(auth), query:_*)

  def get(url: String, auth: Option[String], query: (String, String)*)(implicit app: Application): WSResponse = {
    var request = WS.url(url)

    auth.foreach { auth =>
      request = request.withHeaders(("Authorization" -> auth))
    }

    if (query.length > 0) {
      request = request.withQueryString(query:_*)
    }

    await { request.get }
  }

  def delete(url: String, auth: UUID)(implicit app: Application): WSResponse =
    delete(url, Some(auth.toString))

  def delete(url: String, auth: Option[String])(implicit app: Application): WSResponse = {
    var request = WS.url(url)

    auth.foreach { auth =>
      request = request.withHeaders(("Authorization" -> auth))
    }

    await { request.delete }
  }

}
