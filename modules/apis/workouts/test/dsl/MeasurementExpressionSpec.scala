package dsl.workouts

import org.scalatest._

class MeasurementExpressionSpec
  extends WordSpec
  with MustMatchers
{

  "MeasurementExpression.parse" must {

    "reject a measurement with only a magnitude" in {
      MeasurementExpression.parse("5") mustBe Left("end of input")
    }

    "reject a measurement with more than just a magnitude and a unit" in {
      MeasurementExpression.parse("5 lb man") mustBe Left("end of input expected")
    }

    "reject a measurement with an unknown unit" in {
      MeasurementExpression.parse("5 parsecs") mustBe Left("known unit of measure [deg, m, ft, in, sec, min, lb, kg, pood] expected")
    }

    "accept a decimal measurement" in {
      MeasurementExpression.parse("1.5 pood") mustBe 'right
    }

  }

}
