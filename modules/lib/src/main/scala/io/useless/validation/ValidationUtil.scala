package io.useless.validation

import scala.concurrent.{Future, ExecutionContext}

object ValidationUtil {

  import Validation._

  def mapFuture[T, S](validation: Validation[T])(f: T => Future[S])(implicit ec: ExecutionContext): Future[Validation[S]] = validation match {
    case Success(value) => f(value).map(success)
    case Failure(errors) => Future.successful(failure(errors))
  }

  def flatMapFuture[T, S](validation: Validation[T])(f: T => Future[Validation[S]])(implicit ec: ExecutionContext): Future[Validation[S]] = validation match {
    case Success(value) => f(value)
    case Failure(errors) => Future.successful(failure(errors))
  }

}
