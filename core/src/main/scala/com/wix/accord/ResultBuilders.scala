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

package com.wix.accord

/** Provides a convenience DSL for generating violations:
  *
  * - Rule violations can be created by specifying a value and constraint message as a tuple, for example:
  *   `v -> "must not be empty"`
  * - Group violations can be created by extending the above to include children, as in:
  *   `v -> "does not match any of the rules" -> Seq( v.firstName -> "first name must be empty", ... )`
  */
trait ResultBuilders {
  self: Constraints with Results =>

  import scala.language.implicitConversions

  // TODO reconsider visibility pending subsequent refactoring to DSL module

  /** Converts a tuple of the form value->constraint to a [[com.wix.accord.Results#Results#RuleViolation]]. */
  implicit /*protected*/ def ruleViolationFromTuple( v: ( Any, Constraint ) ): RuleViolation =
    RuleViolation( value = v._1, constraint = v._2, description = None )

  /** Converts an extended tuple of the form value->constraint->ruleSeq to a
    * [[com.wix.accord.Results#Results#GroupViolation]]. */
  implicit /*protected*/ def groupViolationFromTuple( v: ( ( Any, Constraint ), Set[ Violation ] ) ): GroupViolation =
    GroupViolation( value = v._1._1, constraint = v._1._2, description = None, children = v._2 )

  implicit /*protected*/ def staticConstraintToFailureBuilder( c: Constraint ): ( Any => Failure ) =
    v => Failure( Set( RuleViolation( value = v, constraint = c, description = None ) ) )

  /** Wraps a single violation to a [[com.wix.accord.Results#Results#Failure]]. */
  implicit /*protected*/ def singleViolationToFailure[ V <% Violation ]( v: V ): Failure =
    Failure( Set( v ) )


  /** A convenience method that takes a predicate and a violation generator, evaluates the predicate and constructs
    * the appropriate [[com.wix.accord.Results#Results#Result]].
    *
    * @param test The predicate to be evaluated.
    * @param violation A violation generator; only gets executed if the test fails.
    * @return [[com.wix.accord.Results#Results#Success]] if the predicate evaluated successfully, or a
    *        [[com.wix.accord.Results#Results#Failure]] with the generated violation otherwise.
    */
  protected def result( test: => Boolean, violation: => Violation ): Result =
    if ( test ) Success else Failure( Set( violation ) )
}
