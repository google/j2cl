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

/**
 * A (by name) reference to an Array type.
 */
@AutoValue
public abstract class ArrayTypeReference implements TypeReference {
  public abstract int getDimensions();

  public abstract TypeReference getLeafType();

  @Override
  public String getSimpleName() {
    return getLeafType().getSimpleName() + getSuffix();
  }

  @Override
  public String getBinaryName() {
    return getPrefix() + getLeafType().getBinaryName();
  }

  @Override
  public String getCompilationUnitSourceName() {
    return getLeafType().getCompilationUnitSourceName();
  }

  @Override
  public String getSourceName() {
    return getLeafType().getSimpleName() + getSuffix();
  }

  @Override
  public String getPackageName() {
    return getLeafType().getPackageName();
  }

  @Override
  public boolean isArray() {
    return true;
  }

  private String getSuffix() {
    return Strings.repeat("[]", getDimensions());
  }

  private String getPrefix() {
    return Strings.repeat("[", getDimensions());
  }
}
