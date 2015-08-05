package lib.haiku

import scala.concurrent.{Future, ExecutionContext}

import io.useless.Message

object Validation {

  type FailureResult = Map[String, Seq[Message]]

  final case class Success[+T](value: T) extends Validation[T]
  final case class Failure[+T](failureResult: FailureResult) extends Validation[T]

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

  def failure[T](failureResult: FailureResult): Validation[T] = {
    new Failure[T](failureResult)
  }

  def future[T, S](validation: Validation[T])(f: T => Future[S])(implicit ec: ExecutionContext): Future[Validation[S]] = validation match {
    case Success(value) => f(value).map(success)
    case Failure(failureResult) => Future.successful(failure(failureResult))
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

  def fold[S](
    onFailure: FailureResult => S,
    onSuccess: T => S
  ): S = this match {
    case Failure(failureResult) => onFailure(failureResult)
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

  def map[S](f: T => S): Validation[S] = this match {
    case Success(value) => success(f(value))
    case Failure(failureResult) => failure(failureResult)
  }

}
