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
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.Collections;

/**
 * A (by name) reference to a class.
 */
@AutoValue
public abstract class TypeReference {
  public static TypeReference create(
      Iterable<String> packageComponents, Iterable<String> classComponents) {
    return new AutoValue_TypeReference(
        ImmutableList.copyOf(packageComponents), ImmutableList.copyOf(classComponents));
  }

  public abstract ImmutableList<String> getPackageComponents();

  public abstract ImmutableList<String> getClassComponents();

  public String getSimpleName() {
    return Iterables.getLast(getClassComponents());
  }

  public String getBinaryName() {
    return Joiner.on(".")
        .join(
            Iterables.concat(
                getPackageComponents(),
                Collections.singleton(Joiner.on("$").join(getClassComponents()))));
  }

  public String getSourceName() {
    return Joiner.on(".").join(Iterables.concat(getPackageComponents(), getClassComponents()));
  }
}
