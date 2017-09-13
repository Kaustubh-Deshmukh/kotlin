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

package org.jetbrains.kotlin.contracts.visitors

import org.jetbrains.kotlin.contracts.impls.*
import org.jetbrains.kotlin.contracts.structure.ESExpressionVisitor
import org.jetbrains.kotlin.contracts.structure.EffectSchema
import org.jetbrains.kotlin.contracts.structure.calltree.BuiltInOperator
import org.jetbrains.kotlin.contracts.structure.calltree.BuiltInOperatorCall
import org.jetbrains.kotlin.contracts.structure.calltree.Computation

/**
 * Given an [ESExpression], substitutes all variables in it using provided [substitutions] map,
 * and then flattens resulting tree, producing an [EffectSchema], which describes effects
 * of this [ESExpression] with effects of arguments taken into consideration.
 */
class Substitutor(private val substitutions: Map<ESVariable, Computation>) : ESExpressionVisitor<Computation?> {
    override fun visitIs(isOperator: ESIs): Computation? {
        val arg = isOperator.left.accept(this) ?: return null
        return BuiltInOperatorCall(BuiltInOperator.IS, isOperator.functor.apply(arg))
    }

    override fun visitNot(not: ESNot): Computation? {
        val arg = not.arg.accept(this) ?: return null
        return BuiltInOperatorCall(BuiltInOperator.NOT, not.functor.apply(arg))
    }

    override fun visitEqual(equal: ESEqual): Computation? {
        val left = equal.left.accept(this) ?: return null
        val right = equal.right.accept(this) ?: return null
        return BuiltInOperatorCall(BuiltInOperator.EQUALS, equal.functor.apply(listOf(left, right)))
    }

    override fun visitAnd(and: ESAnd): Computation? {
        val left = and.left.accept(this) ?: return null
        val right = and.right.accept(this) ?: return null
        return BuiltInOperatorCall(BuiltInOperator.AND, and.functor.apply(left, right))
    }

    override fun visitOr(or: ESOr): Computation? {
        val left = or.left.accept(this) ?: return null
        val right = or.right.accept(this) ?: return null
        return BuiltInOperatorCall(BuiltInOperator.OR, or.functor.apply(left, right))
    }

    override fun visitVariable(esVariable: ESVariable): Computation?
            = substitutions[esVariable] ?: esVariable

    override fun visitConstant(esConstant: ESConstant): Computation? = esConstant
}