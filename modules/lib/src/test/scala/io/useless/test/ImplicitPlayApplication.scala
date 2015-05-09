package io.useless.test

import org.scalatest.Suite
import play.api.test.FakeApplication
import org.scalatestplus.play.OneAppPerSuite

trait ImplicitPlayApplication extends OneAppPerSuite {

  self: Suite =>

  implicit override lazy val app = new FakeApplication {
    override lazy val routes = None
  }

}
