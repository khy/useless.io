package io.useless.validation

import io.useless.Message

object Validation {

  final case class Success[+T](value: T) extends Validation[T]
  final case class Failure[+T](errors: Seq[Errors]) extends Validation[T]

  def success[T](value: T) = {
    new Success(value)
  }

  def failure[T](key: String, messageKey: String, messageDetails: (String, String)*): Validation[T] = {
    failure(key, Message(messageKey, messageDetails:_*))
  }

  def failure[T](key: String, message: Message): Validation[T] = {
    failure(Seq(Errors.attribute(key, Seq(message))))
  }

  def failure[T](errors: Seq[Errors]): Validation[T] = {
    Failure[T](errors)
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

  def toSuccess: Success[T] = this match {
    case success: Success[T] => success
    case _ => throw new NoSuchElementException
  }

  def toFailure: Failure[T] = this match {
    case failure: Failure[T] => failure
    case _ => throw new NoSuchElementException
  }

  def map[S](f: T => S): Validation[S] = this match {
    case Success(value) => success(f(value))
    case Failure(errors) => failure(errors)
  }

  def flatMap[S](f: T => Validation[S]): Validation[S] = this match {
    case Success(value) => f(value)
    case Failure(errors) => failure(errors)
  }

  def fold[S](
    onFailure: Seq[Errors] => S,
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
      case (Failure(a), Failure(b)) => failure((b ++ a))
    }
  }

}
