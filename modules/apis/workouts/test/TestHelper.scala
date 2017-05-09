package test.workouts

import java.util.UUID

import init.workouts.AbstractApplicationComponents

class TestHelper(
  applicationComponents: AbstractApplicationComponents
) {

  object core {
    import models.workouts.core._

    def buildAbstractTask(
      `while`: WhileExpression = WhileExpression.parse("task.rep < 1").right.get,
      movement: Option[UUID] = None,
      constraints: Option[Seq[Constraint]] = None,
      tasks: Option[Seq[AbstractTask]] = None
    ) = AbstractTask(`while`, movement, constraints, tasks)

    def buildWorkout(
      name: Option[String] = None,
      score: Option[ScoreExpression] = None,
      variables: Option[Seq[FreeVariable]] = None,
      task: AbstractTask = buildAbstractTask()
    ) = Workout(name, score, variables, task)
  }

}
