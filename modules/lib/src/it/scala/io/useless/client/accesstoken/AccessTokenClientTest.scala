package io.useless.client.accesstoken

import java.util.UUID
import org.scalatest.FunSuite
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._

class AccessTokenClientTest
  extends FunSuite
  with    Matchers
{

  test ("An AccessToken can be retrieved") {
    val authClient = AccessTokenClient.instance
    val guid = UUID.fromString("c378d481-5f50-4473-8f32-7f3747b4062c")
    val accessToken = Await.result(authClient.getAccessToken(guid), 1.second)
    accessToken.get.guid should be (guid)
  }

}
