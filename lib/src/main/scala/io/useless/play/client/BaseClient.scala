package io.useless.play.client

import java.util.UUID
import scala.concurrent.Future
import com.ning.http.client.AsyncHttpClientConfig
import play.api.libs.ws.{ WSResponse, WSClient }
import play.api.libs.ws.ning.NingWSClient
import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.json.JsValue

import io.useless.util.Logger

object BaseClient {

  def apply(baseUrl: String, auth: String): BaseClient = {
    new DefaultBaseClient(baseUrl, auth)
  }

}

trait BaseClient {

  def baseUrl: String

  def auth: String

  def get(path: String, query: (String, String)*): Future[WSResponse]

  def post(path: String, body: JsValue): Future[WSResponse]

}


class DefaultBaseClient(
  val baseUrl: String,
  val auth: String
) extends BaseClient with Logger {

  def get(path: String, query: (String, String)*) = {
    logger.info("BaseClient request: GET %s (as [%s])".format(path, auth))
    request(path).withQueryString(query:_*).get
  }

  def post(path: String, body: JsValue) = {
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

  private lazy val client: WSClient = {
    val config = new AsyncHttpClientConfig.Builder().build()
    new NingWSClient(config)
  }

}
