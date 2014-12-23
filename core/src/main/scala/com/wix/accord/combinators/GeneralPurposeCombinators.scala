/*
  Copyright 2013-2014 Wix.com

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.wix.accord.combinators

import com.wix.accord._

trait GeneralPurposeCombinatorConstraints {
  self: Constraints =>

  protected def noMatchingClauseConstraint: Constraint
  protected def invalidGroupConstraint: Constraint
  protected def equalToConstraint[ T ]( to: T ): Constraint
  protected def notEqualToConstraint[ T ]( to: T ): Constraint
}

/** Non type-specific combinators. */
trait GeneralPurposeCombinators extends BaseValidators with ResultBuilders {
  self: Validation with Results with Constraints with GeneralPurposeCombinatorConstraints =>

  /** A combinator that takes a chain of predicates and implements logical AND between them.
    * @param predicates The predicates to chain together.
    * @tparam T The type on which this validator operates.
    */
  class And[ T ]( predicates: Validator[ T ]* ) extends Validator[ T ] {
    def apply( x: T ): Result = predicates.map { _ apply x }.fold( Success ) { _ and _ }
  }

  /** A combinator that takes a chain of predicates and implements logical OR between them. When all predicates
    * fail, a [[com.wix.accord.Results#Results#GroupViolation]] is produced; the predicates comprise the group's children.
    *
    * @param predicates The predicates to chain together.
    * @tparam T The type on which this validator operates.
    */
  class Or[ T ]( predicates: Validator[ T ]* ) extends Validator[ T ] {
    def apply( x: T ) = {
      val results = predicates.map { _ apply x }.toSet
      val failures = results.collect { case Failure( violations ) => violations }.flatten
      result( results exists { _ == Success }, x -> noMatchingClauseConstraint -> failures )
    }
  }

  /** A validator that always fails with a specific violation.
    * @param message The violation message.
    * @tparam T The type on which this validator operates.
    */
  class Fail[ T ]( message: => Constraint ) extends Validator[ T ] {
    def apply( x: T ) = result( test = false, x -> message )
  }

  /** A validator that always succeeds.
    * @tparam T The type on which this validator operates.
    */
  class NilValidator[ T ] extends Validator[ T ] {
    def apply( x: T ) = Success
  }

  /** A validator that succeeds only if the provided object is `null`. */
  class IsNull extends BaseValidator[ AnyRef ]( _ == null, _ -> isNullConstraint )

  /** A validator that succeeds only if the provided object is not `null`. */
  class IsNotNull extends BaseValidator[ AnyRef ]( _ != null, _ -> isNotNullConstraint )

  /** A validator that succeeds only if the validated object is equal to the specified value. Respects nulls
    * and delegates equality checks to [[java.lang.Object.equals]]. */
  class EqualTo[ T ]( to: T ) extends Validator[ T ] {
    private def safeEq( x: T, y: T ) = if ( x == null ) y == null else x equals y
    def apply( x: T ) = result( test = safeEq( x, to ), x -> equalToConstraint( x ) )
  }

  /** A validator that succeeds only if the validated object is not equal to the specified value. Respects nulls
    * and delegates equality checks to [[java.lang.Object.equals]]. */
  class NotEqualTo[ T ]( to: T ) extends Validator[ T ] {
    private def safeEq( x: T, y: T ) = if ( x == null ) y == null else x equals y
    def apply( x: T ) = result( test = !safeEq( x, to ), x -> notEqualToConstraint( x ) )
  }

  /** A validator which merely delegates to another, implicitly available validator. This is necessary for the
    * description generation to work correctly, e.g. in the case where:
    *
    * ```
    * case class Person( firstName: String, lastName: String )
    * case class Classroom( teacher: Person, students: Seq[ Person ] )
    *
    * implicit val personValidator = validator[ Person ] { p =>
    *   p.firstName is notEmpty
    *   p.lastName is notEmpty
    * }
    *
    * implicit val classValidator = validator[ Classroom ] { c =>
    *   c.teacher is valid
    *   c.students.each is valid
    *   c.students have size > 0
    * }
    * ```
    *
    * `c.teacher` actually delegates to the `personValidator`, which means a correct error message would be
    * a [[com.wix.accord.Results#Results#GroupViolation]] aggregating the actual rule violations.
    *
    * @tparam T The object type this validator operates on. An implicit [[com.wix.accord.Validation#Validator]]
    *           over type `T` must be in scope.
    */
  class Valid[ T : Validator ] extends Validator[ T ] {
    def apply( x: T ) =
      if ( x == null )
        nullFailure
      else
        implicitly[ Validator[ T ] ].apply( x ) match {
          case Success => Success
          case Failure( rules ) => Failure( Set( x -> invalidGroupConstraint -> rules ) )
        }
  }
}