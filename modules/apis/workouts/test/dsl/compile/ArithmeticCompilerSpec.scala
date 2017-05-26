package dsl.workouts.compile

import org.scalatest._

class ArithmeticCompilerSpec
  extends WordSpec
  with MustMatchers
{

  import Ast._

  "ArithmeticCompiler.compile" must {

    "handle simple addition" in {
      val arithmetic = ArithmeticCompiler.compile("1 + 2").right.get
      arithmetic mustBe Add(Number(1), Number(2))
    }

    "handle multiple additions" in {
      val arithmetic = ArithmeticCompiler.compile("1 + 2 + 3").right.get
      arithmetic mustBe Add(Add(Number(1), Number(2)), Number(3))
    }

    "handle addition with subtration" in {
      val arithmetic = ArithmeticCompiler.compile("1 + 2 - 3").right.get
      arithmetic mustBe Subtract(Add(Number(1), Number(2)), Number(3))
    }

    "enforce correct order of operations for addition with multiplication" in {
      val arithmetic = ArithmeticCompiler.compile("1 + 2 * 3 + 4").right.get
      arithmetic mustBe Add(Add(Number(1), Multiply(Number(2), Number(3))), Number(4))
    }

    "respect parantheses" in {
      val arithmetic = ArithmeticCompiler.compile("(1 + 2) * (3 + 4)").right.get
      arithmetic mustBe Multiply(Add(Number(1), Number(2)), Add(Number(3), Number(4)))
    }

    "accept simply a variable" in {
      val arithmetic = ArithmeticCompiler.compile("workout.task.time").right.get
      arithmetic mustBe ObjectRef(ObjectRef(ImplicitRef("workout"), "task"), "time")
    }

    "accept an arithmetic expression with variables" in {
      val source = "workout.task.tasks[0].time + workout.task.tasks[2].time"
      val arithmetic = ArithmeticCompiler.compile(source).right.get
      arithmetic mustBe Add(
        ObjectRef(ArrayRef(ObjectRef(ObjectRef(ImplicitRef("workout"), "task"), "tasks"), 0), "time"),
        ObjectRef(ArrayRef(ObjectRef(ObjectRef(ImplicitRef("workout"), "task"), "tasks"), 2), "time")
      )
    }

  }

}
