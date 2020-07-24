/*
 * Copyright 2018 Google Inc.
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

import com.google.j2cl.common.InternalCompilerError;
import java.util.function.Function;
import javax.annotation.Nullable;

/** The type of the null value. */
public class NullType extends TypeDescriptor {
  @Override
  public boolean isNullable() {
    return true;
  }

  @Override
  public TypeDescriptor toNullable() {
    return this;
  }

  @Override
  public TypeDescriptor toNonNullable() {
    throw new InternalCompilerError("Cannot call toNonNullable on the null type.");
  }

  @Override
  public TypeDescriptor toUnparameterizedTypeDescriptor() {
    return this;
  }

  @Override
  public boolean isAssignableTo(TypeDescriptor that) {
    return !that.isPrimitive();
  }

  @Override
  public TypeDescriptor toRawTypeDescriptor() {
    return this;
  }

  @Override
  public boolean canBeReferencedExternally() {
    return false;
  }

  @Nullable
  @Override
  public TypeDeclaration getMetadataTypeDeclaration() {
    return null;
  }

  @Override
  public TypeDescriptor specializeTypeVariables(
      Function<TypeVariable, ? extends TypeDescriptor> replacementTypeArgumentByTypeVariable) {
    return this;
  }

  @Override
  public String getReadableDescription() {
    return "<null type>";
  }

  @Override
  public String getUniqueId() {
    // Return an id that can not clash with those of other type descriptors.
    return ":null";
  }

  // Only instantiated by TypeDescriptors.
  NullType() {}
}
