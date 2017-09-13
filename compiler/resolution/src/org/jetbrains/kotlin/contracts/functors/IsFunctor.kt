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

import org.jetbrains.kotlin.contracts.effects.ESReturns
import org.jetbrains.kotlin.contracts.factories.createClause
import org.jetbrains.kotlin.contracts.impls.*
import org.jetbrains.kotlin.contracts.structure.ESClause
import org.jetbrains.kotlin.contracts.structure.ESExpression
import org.jetbrains.kotlin.contracts.structure.ESFunctor
import org.jetbrains.kotlin.contracts.structure.EffectSchema
import org.jetbrains.kotlin.contracts.structure.calltree.Computation
import org.jetbrains.kotlin.types.KotlinType

class IsFunctor(val type: KotlinType, val isNegated: Boolean) : ESFunctor() {
    override fun doApplication(arguments: List<Computation>): EffectSchema {
        assert(arguments.size == 1, { "Wrong size of arguments list for Unary operator: expected 1, got ${arguments.size}" })
        return apply(arguments[0])
    }

    fun apply(arg: Computation): EffectSchema {
        if (arg is ESValue)
            return EffectSchema(applyToValue(arg, null))
        val clauses = arg.effects.clauses.flatMap {
            if (it.effect !is ESReturns) listOf(it) else applyToValue(it.effect.value, it.condition)
        }
        return EffectSchema(clauses)
    }

    private fun applyToValue(value: ESValue, additionalCondition: ESExpression?): List<ESClause> {
        val trueIs = ESIs(value, this)
        val falseIs = ESIs(value, IsFunctor(type, isNegated.not()))

        val trueResult = createClause(trueIs.and(additionalCondition), ESReturns(true.lift()))
        val falseResult = createClause(falseIs.and(additionalCondition), ESReturns(false.lift()))
        return listOf(trueResult, falseResult)
    }
}