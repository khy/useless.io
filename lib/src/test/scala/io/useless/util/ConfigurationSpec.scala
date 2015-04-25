package io.useless.util

import org.scalatest.FunSpec
import org.scalatest.Matchers

class ConfigurationSpec
  extends FunSpec
  with    Matchers
  with    Configuration
{

  describe ("the default config") {

    it ("should return the canonical useless URL") {
      configuration.getString("useless.core.baseUrl") should be (Some("http://useless.io/core"))
    }

  }

}
