package lib.haiku

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

  def combine[T1, T2, R](v1: Validation[T1], v2: Validation[T2])(f: (T1, T2) => R) = {
    (v1, v2) match {
      case (Success(w1), Success(w2)) => success(f(w1, w2))
      case _ => foldFailure(v1, v2)
    }
  }

  def combine[T1, T2, T3, R](v1: Validation[T1], v2: Validation[T2], v3: Validation[T3])(f: (T1, T2, T3) => R) = {
    (v1, v2, v3) match {
      case (Success(w1), Success(w2), Success(w3)) => success(f(w1, w2, w3))
      case _ => foldFailure(v1, v2, v3)
    }
  }

  private def foldFailure(validations: Validation[_]*): Validation[Nothing] = {
    var failureResult = Map.empty[String, Seq[Message]]

    validations.foreach { validation =>
      validation.fold(
        _failureResult => _failureResult.foreach { case (key, value) =>
          val existing: Seq[Message] = failureResult.get(key).getOrElse(Seq.empty)
          failureResult = failureResult + ((key, existing ++ value))
        },
        success => success
      )
    }

    new Failure(failureResult)
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

}
