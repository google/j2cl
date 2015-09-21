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
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.processors.Visitable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A (by name) reference to a class.
 */
@AutoValue
@Visitable
public abstract class RegularTypeDescriptor extends TypeDescriptor {
  public abstract ImmutableList<String> getPackageComponents();

  public abstract ImmutableList<String> getClassComponents();

  @Override
  public abstract boolean isRaw();

  @Override
  public abstract ImmutableList<TypeDescriptor> getTypeArgumentDescriptors();

  @Override
  public abstract boolean isTypeVariable();

  @Override
  public String getBinaryName() {
    return Joiner.on(".")
        .join(
            Iterables.concat(
                getPackageComponents(),
                Collections.singleton(Joiner.on("$").join(getClassComponents()))));
  }

  @Override
  public String getClassName() {
    if (isPrimitive()) {
      return "$" + getSimpleName();
    }
    if (getSimpleName().equals("?")) {
      return "?";
    }
    if (isTypeVariable()) {
      Preconditions.checkArgument(
          getClassComponents().size() > 1,
          "Type Variable (not including wild card type) should have at least two name components");
      // skip the top level class component for better output readability.
      List<String> nameComponents =
          new ArrayList<>(getClassComponents().subList(1, getClassComponents().size()));

      // move the prefix in the simple name to the class name to avoid collisions between method-
      // level and class-level type variable and avoid variable name starts with a number.
      // concat class components to avoid collisions between type variables in inner/outer class.
      // use '_' instead of '$' because '$' is not allowed in template variable name in closure.
      String simpleName = getSimpleName();
      nameComponents.set(
          nameComponents.size() - 1, simpleName.substring(simpleName.indexOf('_') + 1));
      String prefix = simpleName.substring(0, simpleName.indexOf('_') + 1);

      return prefix + Joiner.on('_').join(nameComponents);
    }
    return Joiner.on('$').join(getClassComponents());
  }

  @Override
  public String getSimpleName() {
    return Iterables.getLast(getClassComponents());
  }

  @Override
  public String getSourceName() {
    // source name is used to do comparison. Add type arguments to its binary name to distinguish
    // parameterized types with the same raw type, and generic types.
    String rawSourceName = getBinaryName();
    String typeArguments =
        getTypeArgumentDescriptors().isEmpty()
            ? ""
            : String.format(
                "<%s>",
                Joiner.on(", ")
                    .join(
                        Lists.transform(
                            getTypeArgumentDescriptors(),
                            new Function<TypeDescriptor, String>() {
                              @Override
                              public String apply(TypeDescriptor typeParameter) {
                                return typeParameter.getSourceName();
                              }
                            })));
    return rawSourceName + typeArguments;
  }

  @Override
  public String getPackageName() {
    return Joiner.on(".").join(getPackageComponents());
  }

  @Override
  public boolean isArray() {
    return false;
  }

  @Override
  public int getDimensions() {
    return 0;
  }

  @Override
  public TypeDescriptor getLeafTypeDescriptor() {
    return null;
  }

  @Override
  public boolean isParameterizedType() {
    return !getTypeArgumentDescriptors().isEmpty();
  }

  /**
   * Raw type of a parameterized type is the type without type parameters or type arguments.
   */
  @Override
  public TypeDescriptor getRawTypeDescriptor() {
    if (isParameterizedType()) {
      return TypeDescriptor.create(this.getPackageComponents(), this.getClassComponents());
    }
    return this;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_RegularTypeDescriptor.visit(processor, this);
  }
}
