/*
 * Copyright 2016 Google Inc.
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

/** Constants supporting manipulation of J2CL AST. */
public class AstUtilConstants {
  public static final String OVERLAY_IMPLEMENTATION_CLASS_SUFFIX = "Overlay";
  public static final String FUNCTIONAL_INTERFACE_ADAPTOR_CLASS_NAME = "LambdaAdaptor";
  public static final String FUNCTIONAL_INTERFACE_JSFUNCTION_CLASS_NAME = "JsFunction";
  public static final String TYPE_VARIABLE_IN_METHOD_PREFIX = "M_";
  public static final String TYPE_VARIABLE_IN_TYPE_PREFIX = "C_";

  private static final ThreadLocal<FieldDescriptor> ARRAY_LENGTH_FIELD_DESCRIPTION =
      ThreadLocal.withInitial(
          () ->
              FieldDescriptor.newBuilder()
                  .setEnclosingTypeDescriptor(TypeDescriptors.get().nativeArray)
                  .setName("length")
                  .setTypeDescriptor(TypeDescriptors.get().primitiveInt)
                  .setStatic(false)
                  .setJsInfo(JsInfo.RAW_FIELD)
                  .build());

  public static FieldDescriptor getArrayLengthFieldDescriptor() {
    return ARRAY_LENGTH_FIELD_DESCRIPTION.get();
  }
}
