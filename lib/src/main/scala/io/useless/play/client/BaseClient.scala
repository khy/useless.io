package io.useless.play.client

import java.util.UUID
import scala.concurrent.Future
import com.ning.http.client.AsyncHttpClientConfig
import play.api.libs.ws.{ WSResponse, WSClient }
import play.api.libs.ws.ning.NingWSClient
import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.json.JsValue

import io.useless.accesstoken.AccessToken
import io.useless.client.{ ClientConfiguration, ClientConfigurationComponent }
import io.useless.util.{ Logger, LoggerComponent }

trait BaseClient
  extends ConfigurableBaseClientComponent
  with    ClientConfiguration
  with    Logger
{

  def baseClient(auth: String): BaseClient = new ConfigurableBaseClient(auth)

}

trait BaseClientComponent {

  def baseClient(auth: String): BaseClient

  trait BaseClient {

    def auth: String

    def get(path: String, query: (String, String)*): Future[WSResponse]

    def post(path: String, body: JsValue): Future[WSResponse]

  }

}

trait ConfigurableBaseClientComponent extends BaseClientComponent {

  self: ClientConfigurationComponent with
        LoggerComponent =>

  class ConfigurableBaseClient(val auth: String) extends BaseClient {

    def this(accessTokenGuid: UUID) = this(accessTokenGuid.toString)

    def this(accessToken: AccessToken) = this(accessToken.guid)

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

    protected def urlForPath(path: String) = clientConfiguration.urlForPath(path)

    private lazy val client: WSClient = {
      val config = new AsyncHttpClientConfig.Builder().build()
      new NingWSClient(config)
    }

  }

}
