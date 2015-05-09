package io.useless.play.client

import java.util.UUID
import scala.concurrent.Future
import com.ning.http.client.AsyncHttpClientConfig
import play.api.Application
import play.api.libs.ws.{ WS, WSResponse, WSClient }
import play.api.libs.json.JsValue

import io.useless.util.Logger

object BaseClient {

  def apply(baseUrl: String, auth: String)(implicit app: Application): BaseClient = {
    new BaseClient(WS.client, baseUrl, auth)
  }

}

class BaseClient(
  client: WSClient,
  val baseUrl: String,
  val auth: String
) extends Logger {

  def get(path: String, query: (String, String)*): Future[WSResponse] = {
    logger.info("BaseClient request: GET %s (as [%s])".format(path, auth))
    request(path).withQueryString(query:_*).get
  }

  def post(path: String, body: JsValue): Future[WSResponse] = {
    logger.info("BaseClient request: POST %s (as [%s])".format(path, auth))
    request(path).post(body)
  }

  private def request(path: String) = {
    client.url(urlForPath(path)).withHeaders(("Authorization", auth))
  }

  private def urlForPath(path: String) = {
    val strippedBaseUrl = "/$".r.replaceAllIn(baseUrl, "")
    val strippedPath = "^/".r.replaceAllIn(path, "")
    strippedBaseUrl + "/" + strippedPath
  }

}
