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

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.j2cl.ast.processors.Visitable;

/**
 * A (by name) reference to an Array type.
 */
@AutoValue
@Visitable
public abstract class ArrayTypeDescriptor extends TypeDescriptor {

  @Override
  public abstract int getDimensions();

  @Override
  public abstract TypeDescriptor getLeafTypeDescriptor();

  @Override
  public TypeDescriptor getComponentTypeDescriptor() {
    return getLeafTypeDescriptor().getForArray(getDimensions() - 1);
  }

  @Override
  public String getBinaryName() {
    return getPrefix() + getLeafTypeDescriptor().getBinaryName();
  }

  @Override
  public String getClassName() {
    return getLeafTypeDescriptor().getClassName() + getSuffix();
  }

  @Override
  public String getSimpleName() {
    return getLeafTypeDescriptor().getSimpleName() + getSuffix();
  }

  @Override
  public String getSourceName() {
    return getLeafTypeDescriptor().getSourceName() + getSuffix();
  }

  @Override
  public String getPackageName() {
    return getLeafTypeDescriptor().getPackageName();
  }

  @Override
  public String getUniqueId() {
    return getLeafTypeDescriptor().getUniqueId() + getSuffix();
  }

  @Override
  public boolean isArray() {
    return true;
  }

  @Override
  public boolean isRaw() {
    return getLeafTypeDescriptor().isRaw();
  }

  private String getSuffix() {
    return Strings.repeat("[]", getDimensions());
  }

  private String getPrefix() {
    return Strings.repeat("[", getDimensions());
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_ArrayTypeDescriptor.visit(processor, this);
  }
}
