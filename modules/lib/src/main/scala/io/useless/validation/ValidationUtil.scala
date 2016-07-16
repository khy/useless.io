package io.useless.validation

import scala.concurrent.{Future, ExecutionContext}

object ValidationUtil {

  import Validation._

  def sequence[T](validations: Seq[Validation[T]]): Validation[Seq[T]] = {
    validations.foldLeft(Validation.success(Seq.empty): Validation[Seq[T]]) { (a, b) =>
      (a ++ b).map { case (seq, value) => seq :+ value }
    }
  }

  def mapFuture[T, S](validation: Validation[T])(f: T => Future[S])(implicit ec: ExecutionContext): Future[Validation[S]] = validation match {
    case Success(value) => f(value).map(success)
    case Failure(errors) => Future.successful(failure(errors))
  }

  def flatMapFuture[T, S](validation: Validation[T])(f: T => Future[Validation[S]])(implicit ec: ExecutionContext): Future[Validation[S]] = validation match {
    case Success(value) => f(value)
    case Failure(errors) => Future.successful(failure(errors))
  }

}
