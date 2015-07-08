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

import com.google.j2cl.ast.processors.Visitable;

/**
 * Character literal node.
 */
@Visitable
public class CharacterLiteral extends Expression {
  private char value;
  private String escapedValue;

  public CharacterLiteral(char value, String escapedValue) {
    this.value = value;
    this.escapedValue = escapedValue;
  }

  public char getValue() {
    return value;
  }

  public String getEscapedValue() {
    return escapedValue;
  }

  @Override
  public CharacterLiteral accept(Processor processor) {
    return Visitor_CharacterLiteral.visit(processor, this);
  }
}
