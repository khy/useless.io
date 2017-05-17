package dsl.workouts.validate

import play.api.libs.json.JsPath
import play.api.data.validation.ValidationError

import models.workouts.core._

object MovementValidator {

  def validateMovement(movement: Movement): Seq[(JsPath, Seq[ValidationError])] = {
    var errors = Seq.empty[(JsPath, Seq[ValidationError])]

    movement.variables.foreach { variables =>
      var seenNames = Seq.empty[String]

      variables.zipWithIndex.foreach { case (variable, index) =>
        if (seenNames.contains(variable.name)) {
          errors = errors :+ ((JsPath \ "variables")(index) \ "name", Seq(ValidationError("duplicate")))
        } else {
          seenNames = seenNames :+ variable.name
        }
      }
    }

    errors
  }

}
