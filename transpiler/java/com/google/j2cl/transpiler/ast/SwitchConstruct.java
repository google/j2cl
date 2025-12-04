/*
 * Copyright 2024 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.ast;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.SourcePosition;
import java.util.List;

/** Interface that describes the common aspects of switch statements and switch expressions. */
public interface SwitchConstruct<T extends SwitchConstruct<T>> extends HasSourcePosition {

  Expression getExpression();

  List<SwitchCase> getCases();

  /** Returns the type of the switch construct. */
  default TypeDescriptor getTypeDescriptor() {
    // If the construct is not an expression and does not implement getTypeDescriptor, it is
    // considered to return void which is inline with top level when expressions in kotlin and
    // switch statements.
    return PrimitiveTypes.VOID;
  }

  /** Returns true if the switch construct has a default case. */
  default boolean hasDefaultCase() {
    return getCases().stream().anyMatch(SwitchCase::isDefault);
  }

  /** Returns true if the expression evaluating to null is handled by any of the cases. */
  boolean allowsNulls();

  /** Returns true if the switch construct has a case that might fallthrough the next. */
  default boolean canFallthrough() {
    return getCases().stream().anyMatch(SwitchCase::canFallthrough);
  }

  Builder<T> toBuilder();

  /** An interface for builders of switch constructs. */
  interface Builder<T extends SwitchConstruct<T>> {
    @CanIgnoreReturnValue
    Builder<T> setExpression(Expression expression);

    @CanIgnoreReturnValue
    Builder<T> setCases(List<SwitchCase> switchCases);

    @CanIgnoreReturnValue
    Builder<T> setAllowsNulls(boolean allowsNulls);

    @CanIgnoreReturnValue
    Builder<T> setSourcePosition(SourcePosition sourcePosition);

    T build();
  }
}
