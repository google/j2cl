/*
 * Copyright 2015 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */
package com.google.j2cl.tools.jsni;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Simple POJO containing all information we need about a jsni method.
 */
class JsniMethod {
  private final String name;
  private final ImmutableList<String> parameterNames;
  private final String body;
  private final boolean isStatic;

  public JsniMethod(String name, List<String> parameterNames, String body, boolean isStatic) {
    this.name = name;
    this.parameterNames = ImmutableList.copyOf(parameterNames);
    this.body = body;
    this.isStatic = isStatic;
  }

  public String getName() {
    return name;
  }

  public ImmutableList<String> getParameterNames() {
    return parameterNames;
  }

  public String getBody() {
    return body;
  }

  public boolean isStatic() {
    return isStatic;
  }

  /**
   * For debug purpose only
   */
  @Override
  public String toString() {
    return (isStatic ? "static " : "") + name + "(" + parameterNames + ") {" + body + "}";
  }
}
