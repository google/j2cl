/*
 * Copyright 2024 Google Inc.
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
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/** Normalizes JsEnum classes to remove the name field initialization. */
public class RemoveNameFromJsEnums extends NormalizationPass {
  @Override
  public void applyTo(Type type) {
    if (!AstUtils.isNonNativeJsEnum(type.getTypeDescriptor())) {
      return;
    }
    type.accept(
        new AbstractRewriter() {
          @Nullable
          @Override
          public Node rewriteField(Field field) {
            if (!field.isEnumField()) {
              return field;
            }

            return removeNameFromInitializer(field);
          }
        });
  }

  /** Creates a field which is constructed only by the value. */
  private static Field removeNameFromInitializer(Field field) {
    checkArgument(field.isEnumField());
    checkState(field.getInitializer() instanceof NewInstance);
    NewInstance enumFieldInitializer = (NewInstance) field.getInitializer();
    return Field.Builder.from(field)
        .setInitializer(
            NewInstance.Builder.from(enumFieldInitializer)
                .setArguments(
                    // Replace the first argument with null.
                    Stream.concat(
                            Stream.of(TypeDescriptors.get().javaLangString.getNullValue()),
                            enumFieldInitializer.getArguments().stream().skip(1))
                        .collect(toImmutableList()))
                .build())
        .build();
  }
}
