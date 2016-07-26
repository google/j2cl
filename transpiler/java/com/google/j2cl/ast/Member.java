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

import com.google.j2cl.ast.annotations.Context;
import com.google.j2cl.ast.annotations.Visitable;

/** Abstract base class for class members. */
@Visitable
@Context
public abstract class Member extends Node {
  public abstract boolean isStatic();

  public boolean isConstructor() {
    return false;
  }

  public boolean isField() {
    return false;
  }

  public boolean isMethod() {
    return false;
  }

  public boolean isInitializerBlock() {
    return false;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_Member.visit(processor, this);
  }
}
