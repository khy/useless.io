package dsl.workouts.compile

import org.scalatest._

import models.workouts.core.UnitOfMeasure
import models.workouts.core.ScoreAst._

class ScoreCompilerSpec
  extends WordSpec
  with MustMatchers
{

  "ScoreCompiler.compile" must {

    "support a score referencing the workout task's time" in {
      val scoreAst = ScoreCompiler.compile("workout.task.time").right.get
      val expression = scoreAst.expression.asInstanceOf[ObjectRef]

      val ref1 = expression.asInstanceOf[ObjectRef]
      ref1.property mustBe "time"

      val ref2 = ref1.ref.asInstanceOf[ObjectRef]
      ref2.property mustBe "task"

      val ref3 = ref2.ref.asInstanceOf[ImplicitRef]
      ref3.property mustBe "workout"
    }

    "support a score referencing one of the workout task's task's times" in {
      val scoreAst = ScoreCompiler.compile("workout.task.tasks[0].reps").right.get
      val expression = scoreAst.expression.asInstanceOf[ObjectRef]

      val ref1 = expression.asInstanceOf[ObjectRef]
      ref1.property mustBe "reps"

      val ref2 = ref1.ref.asInstanceOf[ArrayRef]
      ref2.index mustBe 0

      val ref3 = ref2.ref.asInstanceOf[ObjectRef]
      ref3.property mustBe "tasks"

      val ref4 = ref3.ref.asInstanceOf[ObjectRef]
      ref4.property mustBe "task"

      val ref5 = ref4.ref.asInstanceOf[ImplicitRef]
      ref5.property mustBe "workout"
    }

    "support a score that adds references" in {
      val scoreAst = ScoreCompiler.compile("workout.task.tasks[0].reps + workout.task.tasks[3].reps").right.get
      val expression = scoreAst.expression.asInstanceOf[AdditionOp]
    }

  }

}
