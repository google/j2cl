/*
 * Copyright 2015 Google Inc.
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.StringUtils;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** String literal node. */
@Visitable
public class StringLiteral extends Literal {
  private final String value;

  public StringLiteral(String value) {
    this.value = checkNotNull(value);
  }

  public String getValue() {
    return value;
  }

  @Override
  public String getSourceText() {
    return "\'" + StringUtils.escapeAsWtf16(value) + "\'";
  }

  public ImmutableList<NumberLiteral> toCharLiterals() {
    return value.chars().mapToObj(NumberLiteral::fromChar).collect(toImmutableList());
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_StringLiteral.visit(processor, this);
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return TypeDescriptors.get().javaLangString.toNonNullable();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof StringLiteral)) {
      return false;
    }
    StringLiteral that = (StringLiteral) o;
    return value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public StringLiteral clone() {
    // String literals are value types do not need to actually clone.
    return this;
  }
}
