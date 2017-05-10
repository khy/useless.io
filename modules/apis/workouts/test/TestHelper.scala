package test.workouts

import java.util.UUID
import play.api.test.Helpers._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.useless.accesstoken.AccessToken

import db.workouts._
import init.workouts.AbstractApplicationComponents
import models.workouts._

class TestHelper(
  applicationComponents: AbstractApplicationComponents
) {

  import applicationComponents._

  def createMovementFromJson(
    rawJson: String
  )(implicit accessToken: AccessToken): MovementRecord = await {
    Json.parse(rawJson).validate[models.workouts.core.Movement].fold(
      error => throw new RuntimeException(s"Invalid movement JSON [$error]: $rawJson"),
      movement => movementsService.addMovement(movement, accessToken)
    )
  }.toSuccess.value

  def createMovement(
    name: String = s"movement-${UUID.randomUUID}",
    variables: Option[Seq[core.FreeVariable]] = None
  )(implicit accessToken: AccessToken): MovementRecord = await {
    movementsService.addMovement(core.Movement(name, variables), accessToken)
  }.toSuccess.value

  def buildAbstractTask(
    `while`: core.WhileExpression = core.WhileExpression.parse("task.rep < 1").right.get,
    movement: Option[UUID] = None,
    constraints: Option[Seq[core.Constraint]] = None,
    tasks: Option[Seq[core.AbstractTask]] = None
  ) = core.AbstractTask(`while`, movement, constraints, tasks)

  def buildConstraint(
    variable: String = "Barbell Weight",
    value: core.ConstraintExpression = core.ConstraintExpression.parse("workout.bodyWeight * 1.5").right.get
  ) = core.Constraint(variable, value)

  def buildMovement(
    name: String = "Pull Up",
    variables: Option[Seq[core.FreeVariable]] = None
  ) = core.Movement(name, variables)

  def buildWorkout(
    name: Option[String] = None,
    score: Option[core.ScoreExpression] = None,
    variables: Option[Seq[core.FreeVariable]] = None,
    task: core.AbstractTask = buildAbstractTask()
  ) = core.Workout(name, score, variables, task)

}
