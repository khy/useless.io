package test.util

import scala.concurrent.{ExecutionContext, Future}
import play.api.ApplicationLoader.Context
import io.useless.client.account.{AccountClient, AccountClientComponents}
import io.useless.client.accesstoken.{AccessTokenClient, AccessTokenClientComponents}

import init.books.AbstractApplicationComponents
import models.books.Edition
import clients.books.{EditionClient, ClientComponents}

class TestApplicationComponents(
  context: Context,
  val accountClient: AccountClient,
  val accessTokenClient: AccessTokenClient,
  val editionClient: EditionClient
) extends AbstractApplicationComponents(context)
  with AccountClientComponents
  with AccessTokenClientComponents
  with ClientComponents
