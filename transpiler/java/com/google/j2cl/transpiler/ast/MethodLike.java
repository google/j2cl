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
package com.google.j2cl.transpiler.ast;

import java.util.List;

/** Interface that describes elements that look like method definitions. */
public interface MethodLike extends HasSourcePosition, HasReadableDescription {
  /** Returns the method descriptor related the functional node. */
  MethodDescriptor getDescriptor();

  /** Returns list of parameter declarations. */
  List<Variable> getParameters();

  /** Returns the declaration of the JsVarArgs parameter if any */
  Variable getJsVarargsParameter();
}
