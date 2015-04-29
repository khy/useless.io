package io.useless.play.client

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.{ Json, JsValue, JsResult, JsSuccess, JsError }
import play.api.libs.json.Reads

import io.useless.accesstoken.AccessToken
import io.useless.client._

trait ResourceClient
  extends DefaultResourceClientComponent
  with JsonClient
{

  def resourceClient(auth: String): ResourceClient = {
    val _jsonClient = jsonClient(auth)
    new DefaultResourceClient(_jsonClient)
  }

}

trait ResourceClientComponent {

  def resourceClient(auth: String): ResourceClient

  trait ResourceClient {

    def withAuth(auth: String): ResourceClient

    def get[T](path: String)(implicit reads: Reads[T]): Future[Option[T]]

    def find[T](path: String, query: (String, String)*)(implicit reads: Reads[T]): Future[Page[T]]

    def create[T](path: String, body: JsValue)(implicit reads: Reads[T]): Future[Either[String, T]]

  }

}

trait DefaultResourceClientComponent extends ResourceClientComponent {

  self: DefaultJsonClientComponent with
        ConfigurableBaseClientComponent =>

  class DefaultResourceClient(jsonClient: JsonClient) extends ResourceClient {

    def this(baseClient: BaseClient) = this(new DefaultJsonClient(baseClient))

    def withAuth(auth: String) = {
      new DefaultResourceClient(jsonClient.withAuth(auth))
    }

    def get[T](path: String)(implicit reads: Reads[T]) = {
      jsonClient.get(path).map { optJson =>
        optJson.map { json =>
          Json.fromJson[T](json) match {
            case success: JsSuccess[T] => success.get
            case error: JsError => throw new InvalidJsonResponseException(json, error)
          }
        }
      }
    }

    def find[T](path: String, query: (String, String)*)(implicit reads: Reads[T]) = {
      jsonClient.find(path, query:_*).map { page =>
        page.copy(
          items = page.items.map { json =>
            Json.fromJson[T](json) match {
              case success: JsSuccess[T] => success.get
              case error: JsError => throw new InvalidJsonResponseException(json, error)
            }
          }
        )
      }
    }

    def create[T](path: String, body: JsValue)(implicit reads: Reads[T]) = {
      jsonClient.create(path, body).map { result =>
        result.right.map { json =>
          Json.fromJson[T](json) match {
            case success: JsSuccess[T] => success.get
            case error: JsError => throw new InvalidJsonResponseException(json, error)
          }
        }
      }
    }

  }

}
