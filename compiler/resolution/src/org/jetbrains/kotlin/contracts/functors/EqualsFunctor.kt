/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.contracts.functors

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.contracts.effects.ESReturns
import org.jetbrains.kotlin.contracts.factories.createClause
import org.jetbrains.kotlin.contracts.impls.*
import org.jetbrains.kotlin.contracts.structure.EMPTY_SCHEMA
import org.jetbrains.kotlin.contracts.structure.ESClause
import org.jetbrains.kotlin.contracts.structure.ESFunctor
import org.jetbrains.kotlin.contracts.structure.EffectSchema
import org.jetbrains.kotlin.contracts.structure.calltree.Computation

class EqualsFunctor(val isNegated: Boolean) : ESFunctor() {
    /*
        Equals is a bit tricky case to produce clauses, because is we want to emit "Returns(true)"-clause,
        then we have to guarantee that we know *all* cases when 'true' could've been returned, and join
        them with OR properly.
        To understand this, consider following example:

            foo(x) == bar(x)
            Effects of foo(x): "Returns(true) -> x is String"
            Effects of bar(x): "Returns(true) -> x is Int"

        Of course, we can't say that the whole expression has effect "Returns(true) -> x is String && x is Int"
        because it could result in 'true' also when 'foo(x) == false' and 'bar(x) == false', and we don't know
        anything about such cases.

        We don't want to code here fair analysis for general cases, because it's too complex. Instead, we just
        check some specific cases, which are useful enough in practice
     */
    override fun doApplication(arguments: List<Computation>): EffectSchema {
        assert(arguments.size == 2) { "Equals functor expected 2 arguments, got ${arguments.size}" }

        // TODO: AnnotationConstructorCaller kills this with implicit receiver. Investigate, how.
        if (arguments.size != 2) return EMPTY_SCHEMA
        return apply(arguments[0], arguments[1])
    }

    fun apply(left: Computation, right: Computation): EffectSchema {
        // First, check if both arguments are values: then we can produce both 'true' and 'false' clauses
        if (left is ESValue && right is ESValue) {
            return equateValues(left, right)
        }

        // Second, check is at least one of argument is Constant: then we can produce 'true'-clause and maybe even 'false'
        if (left is ESConstant) {
            return equateCallAndConstant(right, left)
        }
        if (right is ESConstant) {
            return equateCallAndConstant(left, right)
        }

        // Otherwise, don't even try to produce something. We can improve this in future, if we would like to
        return EMPTY_SCHEMA
    }

    private fun equateCallAndConstant(call: Computation, constant: ESConstant): EffectSchema {
        val resultingClauses = mutableListOf<ESClause>()

        for (clause in call.effects.clauses) {
            val effect = clause.effect

            if (effect !is ESReturns) {
                resultingClauses.add(clause)
                continue
            }

            val clauseReturnValue = effect.value

            if (clauseReturnValue == constant) {
                val trueClause = createClause(clause.condition, ESReturns(isNegated.not().lift()))
                resultingClauses.add(trueClause)
            }

            if (clauseReturnValue != constant && isSafeToProduceFalse(call, clauseReturnValue, constant)) {
                val falseClause = createClause(clause.condition, ESReturns(isNegated.lift()))
                resultingClauses.add(falseClause)
            }
        }

        return EffectSchema(resultingClauses)
    }

    // It is safe to produce false if we're comparing types which are isomorphic to Boolean. For such types we can be sure, that
    // if leftConstant != rightConstant, then this is the only way to produce 'false'.
    private fun isSafeToProduceFalse(leftCall: Computation, leftConstant: ESConstant, rightConstant: ESConstant): Boolean = when {
        // Comparison of Boolean
        rightConstant.type == DefaultBuiltIns.Instance.booleanType && leftCall.type == DefaultBuiltIns.Instance.booleanType -> true

        // Comparison of NULL/NOT_NULL, which is essentially Boolean
        leftConstant.isNullConstant() && rightConstant.isNullConstant() -> true

        else -> false
    }

    private fun equateValues(left: ESValue, right: ESValue): EffectSchema {
        return EffectSchema(
                createClause(ESEqual(left, right, isNegated), ESReturns(true.lift())),
                createClause(ESEqual(left, right, isNegated.not()), ESReturns(false.lift()))
        )
    }
}