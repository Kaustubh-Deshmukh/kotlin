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

import org.jetbrains.kotlin.contracts.effects.ESCalls
import org.jetbrains.kotlin.contracts.effects.ESReturns
import org.jetbrains.kotlin.contracts.factories.createClause
import org.jetbrains.kotlin.contracts.impls.ESConstant
import org.jetbrains.kotlin.contracts.impls.ESValue
import org.jetbrains.kotlin.contracts.impls.ESVariable
import org.jetbrains.kotlin.contracts.structure.*
import org.jetbrains.kotlin.contracts.structure.calltree.Computation
import org.jetbrains.kotlin.contracts.visitors.Substitutor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.utils.addIfNotNull

class SubstitutingFunctor(private val basicSchema: EffectSchema, private val ownerFunction: FunctionDescriptor) : ESFunctor() {
    override fun doApplication(arguments: List<Computation>): EffectSchema {
        val receiver = listOfNotNull(ownerFunction.dispatchReceiverParameter?.toESVariable(), ownerFunction.extensionReceiverParameter?.toESVariable())
        val parameters = receiver + ownerFunction.valueParameters.map { it.toESVariable() }

        assert(parameters.size == arguments.size) {
            "Arguments and parameters size mismatch: arguments.size = ${arguments.size}, parameters.size = ${parameters.size}"
        }

        val substitutions = parameters.zip(arguments).toMap()
        val substitutor = Substitutor(substitutions)
        val substitutedClauses = mutableListOf<ESClause>()

        for (clause in basicSchema.clauses) {
            // TODO: ugly a bit
            if (clause.effect is ESCalls) {
                val subsitutionForCallable = substitutions[clause.effect.callable] as? ESValue ?: continue
                substitutedClauses += clause.replaceEffect(ESCalls(subsitutionForCallable, clause.effect.kind))

            }

            clause.condition.accept(substitutor)?.effects?.clauses?.forEach { substitutedClauses.addIfNotNull(combine(clause.effect, it)) }
        }

        return EffectSchema(substitutedClauses)
    }

    private fun combine(effect: ESEffect, substitutedCondition: ESClause): ESClause? {
        val effectFromCondition = substitutedCondition.effect
        if (effectFromCondition !is ESReturns || effectFromCondition.value == ESConstant.WILDCARD) return substitutedCondition

        if (effectFromCondition.value != ESConstant.TRUE) return null

        return createClause(substitutedCondition.condition, effect)
    }

    private fun ValueDescriptor.toESVariable(): ESVariable = ESVariable(this)
}