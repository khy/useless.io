package dsl.workouts

import play.api.libs.json.JsPath
import play.api.data.validation.ValidationError

import models.workouts.core.Workout

object WorkoutValidator {

  def validateWorkout(workout: Workout): Seq[(JsPath, Seq[ValidationError])] = {
    Nil
  }

}
