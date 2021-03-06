package controllers.budget

import java.util.UUID
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.joda.time.DateTime
import io.useless.play.json.validation.ErrorsJson._
import io.useless.play.pagination.PaginationController

import controllers.budget.auth.Auth
import services.budget.AccountsService
import models.budget.AccountType
import models.budget.JsonImplicits._

object AccountsController extends Controller with PaginationController {

  val accountsService = AccountsService.default()

  def index = Auth.async { implicit request =>
    val contextGuids = request.queryString.get("context").
      map { rawUuids => rawUuids.map(UUID.fromString) }

    withRawPaginationParams { rawPaginationParams =>
      accountsService.findAccounts(
        contextGuids = contextGuids,
        userGuids = Some(Seq(request.accessToken.resourceOwner.guid)),
        rawPaginationParams = rawPaginationParams
      ).map { result =>
        result.fold(
          errors => Conflict(Json.toJson(errors)),
          accounts => paginatedResult(routes.AccountsController.index, accounts)
        )
      }
    }
  }

  case class CreateData(
    contextGuid: UUID,
    accountType: AccountType,
    name: String,
    initialBalance: BigDecimal
  )
  private implicit val cdr = Json.reads[CreateData]

  def create = Auth.async(parse.json) { request =>
    request.body.validate[CreateData].fold(
      error => Future.successful(Conflict(error.toString)),
      data => accountsService.createAccount(
        contextGuid = data.contextGuid,
        accountType = data.accountType,
        name = data.name,
        initialBalance = data.initialBalance,
        accessToken = request.accessToken
      ).map { result =>
        result.fold(
          errors => Conflict(Json.toJson(errors)),
          account => Created(Json.toJson(account))
        )
      }
    )
  }

}
