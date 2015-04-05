package io.useless.client

import scala.util.DynamicVariable

trait Mockable[T] {

  private val dynamicVariable = new DynamicVariable[Option[T]](None)

  def mock = dynamicVariable.value

  def setMock(mock: T) = dynamicVariable.value_=(Some(mock))

  def withMock[S](thunk: => S)(implicit mock: T): S = {
    withMock(mock)(thunk)
  }

  def withMock[S](mock: T)(thunk: => S): S = {
    dynamicVariable.withValue(Some(mock))(thunk)
  }

}
