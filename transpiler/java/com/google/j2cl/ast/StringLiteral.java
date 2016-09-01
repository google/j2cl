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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.J2clUtils;

/**
 * String literal node.
 */
@Visitable
public class StringLiteral extends Expression {
  private final String escapedValue;

  public StringLiteral(String escapedValue) {
    this.escapedValue = checkNotNull(escapedValue);
    checkArgument(
        escapedValue.startsWith("\"") && escapedValue.endsWith("\""),
        "The 'escapedValue' argument must be escaped (and conform to JDT's definition "
            + "of escaped) which that means that it also includes it's own starting "
            + "and ending \"s.");
  }

  public String getEscapedValue() {
    return escapedValue;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_StringLiteral.visit(processor, this);
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return TypeDescriptors.get().javaLangString;
  }

  @Override
  public StringLiteral clone() {
    // String literals are value types do not need to actually clone.
    return this;
  }

  /**
   * Creates a StringLiteral from plain text.
   */
  public static StringLiteral fromPlainText(String string) {
    return new StringLiteral("\"" + J2clUtils.escapeJavaString(string) + "\"");
  }
}
