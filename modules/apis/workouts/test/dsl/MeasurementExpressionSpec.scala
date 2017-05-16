package dsl.workouts

import org.scalatest._

import models.workouts.core.UnitOfMeasure

class MeasurementExpressionSpec
  extends WordSpec
  with MustMatchers
{

  "MeasurementExpression.parse" must {

    "reject a measurement with only a magnitude" in {
      val error = MeasurementExpression.parse("5").left.get
      error.column mustBe 0
      error.message mustBe "end of input"
    }

    "reject a measurement with more than just a magnitude and a unit" in {
      val error = MeasurementExpression.parse("5 lb man").left.get
      error.column mustBe 6
      error.message mustBe "end of input expected"
    }

    "reject a measurement with an unknown unit" in {
      val error = MeasurementExpression.parse("5 parsecs").left.get
      error.column mustBe 3
      error.message mustBe "known unit of measure [deg, m, ft, in, sec, min, lb, kg, pood] expected"
    }

    "accept a decimal measurement" in {
      val expression = MeasurementExpression.parse("1.5 pood").right.get
      expression.magnitude mustBe BigDecimal(1.5)
      expression.unitOfMeasure mustBe UnitOfMeasure.Pood
    }

  }

}
