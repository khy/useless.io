package dsl.workouts.compile

import org.scalatest._

class VariableCompilerSpec
  extends WordSpec
  with MustMatchers
{

  import Ast._

  "VariableCompiler.compile" must {

    "support a score referencing the workout task's time" in {
      val variableAst = VariableCompiler.compile("workout.task.time").right.get
      val var1 = variableAst.variable.asInstanceOf[ObjectRef]

      val var2 = var1.asInstanceOf[ObjectRef]
      var2.property mustBe "time"

      val var3 = var2.variable.asInstanceOf[ObjectRef]
      var3.property mustBe "task"

      val var4 = var3.variable.asInstanceOf[ImplicitRef]
      var4.property mustBe "workout"
    }

    "support a score referencing one of the workout task's task's times" in {
      val scoreAst = VariableCompiler.compile("workout.task.tasks[0].reps").right.get
      val var1 = scoreAst.variable.asInstanceOf[ObjectRef]

      val var2 = var1.asInstanceOf[ObjectRef]
      var2.property mustBe "reps"

      val var3 = var2.variable.asInstanceOf[ArrayRef]
      var3.index mustBe 0

      val var4 = var3.variable.asInstanceOf[ObjectRef]
      var4.property mustBe "tasks"

      val var5 = var4.variable.asInstanceOf[ObjectRef]
      var5.property mustBe "task"

      val var6 = var5.variable.asInstanceOf[ImplicitRef]
      var6.property mustBe "workout"
    }

  }

}
