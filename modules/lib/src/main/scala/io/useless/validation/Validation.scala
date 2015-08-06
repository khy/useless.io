package io.useless.validation

import scala.concurrent.{Future, ExecutionContext}

import io.useless.Message

object Validation {

  final case class Success[+T](value: T) extends Validation[T]
  final case class Failure[+T](errors: Errors) extends Validation[T]

  type Errors = Map[String, Seq[Message]]

  def success[T](value: T) = {
    new Success(value)
  }

  def failure[T](
    key: String,
    messageKey: String,
    messageDetails: (String, String)*
  ): Validation[T] = {
    val message = Message(messageKey, messageDetails:_*)
    failure(Map(key -> Seq(message)))
  }

  def failure[T](errors: Errors): Validation[T] = {
    new Failure[T](errors)
  }

  def future[T, S](validation: Validation[T])(f: T => Future[S])(implicit ec: ExecutionContext): Future[Validation[S]] = validation match {
    case Success(value) => f(value).map(success)
    case Failure(errors) => Future.successful(failure(errors))
  }

}

sealed trait Validation[+T] {

  import Validation._

  def isSuccess: Boolean = this match {
    case Failure(_) => false
    case Success(_) => true
  }

  def isFailure: Boolean = this match {
    case Failure(_) => true
    case Success(_) => false
  }

  def toSuccess: Option[Success[T]] = this match {
    case success: Success[T] => Some(success)
    case _ => None
  }

  def toFailure: Option[Failure[T]] = this match {
    case failure: Failure[T] => Some(failure)
    case _ => None
  }

  def map[S](f: T => S): Validation[S] = this match {
    case Success(value) => success(f(value))
    case Failure(errors) => failure(errors)
  }

  def fold[S](
    onFailure: Errors => S,
    onSuccess: T => S
  ): S = this match {
    case Failure(errors) => onFailure(errors)
    case Success(value) => onSuccess(value)
  }

  def ++[S](other: Validation[S]): Validation[(T, S)] = {
    (this, other) match {
      case (Success(a), Success(b)) => success((a, b))
      case (Success(a), Failure(b)) => failure(b)
      case (Failure(a), Success(b)) => failure(a)
      case (Failure(a), Failure(b)) => failure((b ++ a).map { case (key, messages) =>
        key -> (messages ++ b.get(key).getOrElse(Seq.empty))
      })
    }
  }

}
