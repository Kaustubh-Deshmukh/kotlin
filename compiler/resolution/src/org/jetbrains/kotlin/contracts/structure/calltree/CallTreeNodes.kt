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

package org.jetbrains.kotlin.contracts.structure.calltree

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.contracts.structure.EMPTY_SCHEMA
import org.jetbrains.kotlin.contracts.structure.EffectSchema
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.types.KotlinType

/**
 * Classes in this file are nodes of Call Tree-structure, which is
 * essentially tree of nested calls and their arguments
 */

interface Computation {
    val type: KotlinType?
    val effects: EffectSchema
}

abstract class AbstractComputation(override val type: KotlinType?, override val effects: EffectSchema) : Computation

class FunctionCall(val sourceDescriptor: FunctionDescriptor, effects: EffectSchema) : AbstractComputation(sourceDescriptor.returnType, effects)

class BuiltInOperatorCall(val operatorKind: BuiltInOperator, effects: EffectSchema) : AbstractComputation(DefaultBuiltIns.Instance.booleanType, effects)

enum class BuiltInOperator {
    EQUALS,
    AND,
    OR,
    NOT,
    IS
}

object UNKNOWN_COMPUTATION : AbstractComputation(DefaultBuiltIns.Instance.anyType, EMPTY_SCHEMA)
