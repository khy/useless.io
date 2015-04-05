package io.useless.test

import scala.concurrent.Awaitable
import scala.concurrent.duration._

object Await {

  def Duration = 1.second

  def apply[T](awaitable: Awaitable[T]) = result(awaitable)

  def result[T](awaitable: Awaitable[T]): T = {
    scala.concurrent.Await.result(awaitable, Duration)
  }

}
