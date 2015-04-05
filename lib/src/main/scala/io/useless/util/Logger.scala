package io.useless.util

trait Logger
  extends DefaultLoggerComponent

trait LoggerComponent {

  def logger: play.api.LoggerLike

}

trait DefaultLoggerComponent extends LoggerComponent {

  lazy val logger = play.api.Logger("useless")

}
