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
package com.google.j2cl.ast;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.J2clUtils;

/**
 * Character literal node.
 */
@Visitable
public class CharacterLiteral extends Expression {
  private final char value;
  private final String escapedValue;

  public CharacterLiteral(char value) {
    this(value, "\"" + J2clUtils.escapeJavaString(String.valueOf(value)) + "\"");
  }

  public CharacterLiteral(char value, String escapedValue) {
    this.value = value;
    this.escapedValue = checkNotNull(escapedValue);
  }

  public char getValue() {
    return value;
  }

  public String getEscapedValue() {
    return escapedValue;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return TypeDescriptors.get().primitiveChar;
  }

  @Override
  public CharacterLiteral clone() {
    // Character literals are value types do not need to be actually cloned.
    return this;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_CharacterLiteral.visit(processor, this);
  }
}
