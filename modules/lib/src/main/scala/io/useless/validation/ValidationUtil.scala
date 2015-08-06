package io.useless.validation

import scala.concurrent.{Future, ExecutionContext}

object ValidationUtil {

  import Validation._

  def future[T, S](validation: Validation[T])(f: T => Future[S])(implicit ec: ExecutionContext): Future[Validation[S]] = validation match {
    case Success(value) => f(value).map(success)
    case Failure(errors) => Future.successful(failure(errors))
  }

}
