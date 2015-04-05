package io.useless.play.authentication

import play.api.mvc.{ Request, Result, Results }

trait RejectorComponent {

  val rejector: Rejector

  trait Rejector {

    def unauthenticated[A](request: Request[A]): Result

    def unauthorized[A](request: Request[A]): Result

  }

}

trait ApiRejectorComponent extends RejectorComponent {

  class ApiRejector extends Rejector {

    def unauthenticated[A](request: Request[A]) = Results.Unauthorized

    def unauthorized[A](request: Request[A]) = Results.Unauthorized

  }

}
