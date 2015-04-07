package models.core.account

import java.util.UUID

class AccountException(msg: String) extends RuntimeException(msg)

class InvalidAccountStateException(accountGuid: UUID)
  extends AccountException(s"Account with GUID [$accountGuid] has invalid state: must define exactly one of api, app or user.")

class FailedAccountReloadException(message: String)
  extends AccountException(s"The account could not be reloaded: $message")
