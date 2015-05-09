package io.useless.play.client

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Application
import play.api.libs.json._
import play.api.libs.json.Reads

import io.useless.client._

object JsonClient {

  def apply(baseUrl: String, auth: String)(implicit app: Application): JsonClient = {
    val baseClient = BaseClient(baseUrl, auth)
    new JsonClient(baseClient)
  }

}

class JsonClient(baseClient: BaseClient) {

  def get(path: String): Future[Option[JsValue]] = {
    baseClient.get(path).map { response =>
      response.status match {
        case 200 => Some(response.json)
        case 404 => None
        case 401 => throw new UnauthorizedException(baseClient.auth)
        case 500 => throw new ServerErrorException(response.body)
        case status: Int => throw new UnexpectedStatusException(status, path)
      }
    }
  }

  def find(path: String, query: (String, String)*): Future[Page[JsValue]] = {
    baseClient.get(path, query:_*).map { response =>
      response.status match {
        case 200 => Json.fromJson[Seq[JsValue]](response.json) match {
          case success: JsSuccess[_] => Page(success.get, response)
          case error: JsError => throw new InvalidJsonResponseException(response.json, error)
        }
        case 401 => throw new UnauthorizedException(baseClient.auth)
        case 500 => throw new ServerErrorException(response.body)
        case status: Int => throw new UnexpectedStatusException(status, path)
      }
    }
  }

  def create(path: String, body: JsValue): Future[Either[String, JsValue]] = {
    baseClient.post(path, body).map { response =>
      response.status match {
        case 201 => Right(response.json)
        case 409 => Left(response.body)
        case 422 => Left(response.body)
        case 401 => throw new UnauthorizedException(baseClient.auth)
        case 500 => throw new ServerErrorException(response.body)
        case status: Int => throw new UnexpectedStatusException(status, path)
      }
    }
  }

}
