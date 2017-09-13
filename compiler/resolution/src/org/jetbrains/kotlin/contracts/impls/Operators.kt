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

package org.jetbrains.kotlin.contracts.impls

import org.jetbrains.kotlin.contracts.functors.*
import org.jetbrains.kotlin.contracts.structure.ESExpression
import org.jetbrains.kotlin.contracts.structure.ESExpressionVisitor
import org.jetbrains.kotlin.contracts.structure.ESOperator

class ESAnd(val left: ESExpression, val right: ESExpression, override val functor: AndFunctor = AndFunctor()): ESOperator {
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitAnd(this)
}

class ESOr(val left: ESExpression, val right: ESExpression, override val functor: OrFunctor = OrFunctor()): ESOperator {
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitOr(this)
}

class ESNot(val arg: ESExpression, override val functor: NotFunctor = NotFunctor()): ESOperator {
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitNot(this)

}

class ESIs(val left: ESValue, override val functor: IsFunctor): ESOperator {
    val type = functor.type
    val isNegated = functor.isNegated

    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitIs(this)

}

class ESEqual(val left: ESValue, val right: ESValue, override val functor: EqualsFunctor): ESOperator {
    constructor(left: ESValue, right: ESValue, isInverted: Boolean) : this(left, right, EqualsFunctor(isInverted))
    val isNegated = functor.isNegated

    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitEqual(this)
}

fun ESExpression.and(other: ESExpression?): ESExpression = if (other == null) this else ESAnd(this, other)