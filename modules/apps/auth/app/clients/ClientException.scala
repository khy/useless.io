package clients.auth

import io.useless.accesstoken.AccessToken
import io.useless.account.Account

class ClientException(msg: String) extends RuntimeException(msg)

class UnexpectedAccountTypeException(account: Account, expectedType: String)
  extends ClientException(s"Expected account [$account.guid] to be a $expectedType")

class UnexpectedAccessTokenTypeException(accessToken: AccessToken, expectedType: String)
  extends ClientException(s"Expected access token [$accessToken.guid] to be a $expectedType")
