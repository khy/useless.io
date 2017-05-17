package dsl.workouts.validate

import play.api.libs.json.JsPath

import models.workouts.core._
import test.workouts.IntegrationSpec

class MovementValidatorSpec extends IntegrationSpec {

  import testHelper._

  "MovementValidator.validateMovement" must {

    "accept the movement built by TestHelper" in {
      val movement = buildMovement()
      MovementValidator.validateMovement(movement) mustBe Nil
    }

    "reject a movement with duplicate variable names" in {
      val movement = buildMovement(
        name = "Clean",
        variables = Some(Seq(
          FreeVariable(name = "Barbell Weight", dimension = Dimension.Weight),
          FreeVariable(name = "Barbell Weight", dimension = Dimension.Weight)
        ))
      )

      val (jsPath, errors) = MovementValidator.validateMovement(movement).head
      jsPath mustBe (JsPath \ "variables")(1) \ "name"
      errors.head.message mustBe "duplicate"
    }

    "return multiple errors, if necessary" in {
      val movement = buildMovement(
        name = "Wall Ball",
        variables = Some(Seq(
          FreeVariable(name = "Target Height", dimension = Dimension.Distance),
          FreeVariable(name = "Target Height", dimension = Dimension.Distance),
          FreeVariable(name = "Target Height", dimension = Dimension.Distance),
          FreeVariable(name = "Ball Weight", dimension = Dimension.Weight),
          FreeVariable(name = "Ball Weight", dimension = Dimension.Weight)
        ))
      )

      val errors = MovementValidator.validateMovement(movement)
      errors.length mustBe 3
    }

  }

}
