package dsl.workouts.compile

import org.scalatest._

class ArithmeticCompilerSpec
  extends WordSpec
  with MustMatchers
{

  import ArithmeticAst._

  "ArithmeticCompiler.compile" must {

    "handle simple addition" in {
      val expression = ArithmeticCompiler.compile("1 + 2").right.get.expression
      expression mustBe Add(Number(1), Number(2))
    }

    "handle multiple additions" in {
      val expression = ArithmeticCompiler.compile("1 + 2 + 3").right.get.expression
      expression mustBe Add(Add(Number(1), Number(2)), Number(3))
    }

    "handle addition with subtration" in {
      val expression = ArithmeticCompiler.compile("1 + 2 - 3").right.get.expression
      expression mustBe Subtract(Add(Number(1), Number(2)), Number(3))
    }

    "enforce correct order of operations for addition with multiplication" in {
      val expression = ArithmeticCompiler.compile("1 + 2 * 3 + 4").right.get.expression
      expression mustBe Add(Add(Number(1), Multiply(Number(2), Number(3))), Number(4))
    }

    "respect parantheses" in {
      val expression = ArithmeticCompiler.compile("(1 + 2) * (3 + 4)").right.get.expression
      expression mustBe Multiply(Add(Number(1), Number(2)), Add(Number(3), Number(4)))
    }

  }

}
