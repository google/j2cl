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

import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.ast.common.HasJsNameInfo;

/** Abstract base class for member descriptors. */
@Visitable
public abstract class MemberDescriptor extends Node implements HasJsNameInfo {

  public abstract JsInfo getJsInfo();

  public abstract TypeDescriptor getEnclosingClassTypeDescriptor();

  public abstract String getName();

  @Override
  public abstract boolean isNative();

  public abstract boolean isStatic();

  public abstract boolean isPolymorphic();

  @Override
  public String getJsName() {
    String jsName = getJsInfo().getJsName();
    return jsName == null ? getName() : jsName;
  }

  @Override
  public String getJsNamespace() {
    String jsNamespace = getJsInfo().getJsNamespace();
    return jsNamespace == null ? getEnclosingClassTypeDescriptor().getJsNamespace() : jsNamespace;
  }

  public boolean hasJsNamespace() {
    return getJsInfo().getJsNamespace() != null;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_MemberDescriptor.visit(processor, this);
  }
}
