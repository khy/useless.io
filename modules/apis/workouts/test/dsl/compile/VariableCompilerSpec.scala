package dsl.workouts.compile

import org.scalatest._

class VariableCompilerSpec
  extends WordSpec
  with MustMatchers
{

  import VariableAst._

  "VariableCompiler.compile" must {

    "support a score referencing the workout task's time" in {
      val scoreAst = VariableCompiler.compile("workout.task.time").right.get
      val ref = scoreAst.ref.asInstanceOf[ObjectRef]

      val ref1 = ref.asInstanceOf[ObjectRef]
      ref1.property mustBe "time"

      val ref2 = ref1.ref.asInstanceOf[ObjectRef]
      ref2.property mustBe "task"

      val ref3 = ref2.ref.asInstanceOf[ImplicitRef]
      ref3.property mustBe "workout"
    }

    "support a score referencing one of the workout task's task's times" in {
      val scoreAst = VariableCompiler.compile("workout.task.tasks[0].reps").right.get
      val ref = scoreAst.ref.asInstanceOf[ObjectRef]

      val ref1 = ref.asInstanceOf[ObjectRef]
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

  }

}
