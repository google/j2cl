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

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Base class for type reference.
 */
@Visitable
public abstract class TypeReference extends Expression implements Comparable<TypeReference> {
  public static final TypeReference VOID_TYPEREF =
      RegularTypeReference.create(new ArrayList<String>(), Arrays.asList("void"), "");
  public static final TypeReference OBJECT_TYPEREF =
      RegularTypeReference.create(Arrays.asList("java", "lang"), Arrays.asList("Object"), "Object");

  /**
   * Implements devirtualized Object methods that allow us to special case some behavior and treat
   * some native JS classes as the same and some matching Java classes.
   */
  public static final TypeReference OBJECTS_TYPEREF =
      RegularTypeReference.create(
          Arrays.asList("vmbootstrap"), Arrays.asList("Objects"), "Objects");

  public abstract String getBinaryName();

  public abstract String getClassName();

  public abstract String getCompilationUnitSourceName();

  public abstract String getSimpleName();

  public abstract String getSourceName();

  public abstract String getPackageName();

  public abstract boolean isArray();

  public abstract int getDimensions();

  public abstract TypeReference getLeafTypeRef();

  public String computeModuleName() {
    return "gen." + getCompilationUnitSourceName() + "Module";
  }

  @Override
  public int compareTo(TypeReference that) {
    return getBinaryName().compareTo(that.getBinaryName());
  }

  @Override
  public TypeReference accept(Processor processor) {
    return Visitor_TypeReference.visit(processor, this);
  }
}
