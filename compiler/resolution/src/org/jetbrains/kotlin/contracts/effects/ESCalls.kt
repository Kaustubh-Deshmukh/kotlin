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

package org.jetbrains.kotlin.contracts.effects

import org.jetbrains.kotlin.contracts.impls.ESValue
import org.jetbrains.kotlin.descriptors.contracts.effects.InvocationKind
import org.jetbrains.kotlin.contracts.structure.ESEffect
import org.jetbrains.kotlin.contracts.impls.ESVariable

data class ESCalls(val callable: ESValue, val kind: InvocationKind): ESEffect {
    override fun isImplies(other: ESEffect): Boolean? {
        if (other !is ESCalls) return null

        if (callable != other.callable) return null

        return kind == other.kind
    }

}
fun InvocationKind.isDefinitelyVisited(): Boolean = this == InvocationKind.EXACTLY_ONCE || this == InvocationKind.AT_LEAST_ONCE
fun InvocationKind.canBeRevisited(): Boolean = this == InvocationKind.UNKNOWN || this == InvocationKind.AT_LEAST_ONCE