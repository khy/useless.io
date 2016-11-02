package test.workouts

import scala.concurrent.{ExecutionContext, Future}
import play.api.ApplicationLoader.Context
import io.useless.client.account.{AccountClient, AccountClientComponents}
import io.useless.client.accesstoken.{AccessTokenClient, AccessTokenClientComponents}

import init.workouts.AbstractApplicationComponents

class TestApplicationComponents(
  context: Context,
  val accountClient: AccountClient,
  val accessTokenClient: AccessTokenClient
) extends AbstractApplicationComponents(context)
  with AccountClientComponents
  with AccessTokenClientComponents
