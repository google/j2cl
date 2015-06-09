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

import java.util.List;

/**
 * A node that represents a Java Type declaration in the compilation unit.
 */
public class JavaType extends Node {

  /**
   * Describes the kind of the Java type.
   */
  public enum Kind { CLASS, INTERFACE, ENUM }
  private Kind kind;

  private String name;
  @Visitable
  private List<Node> fields;

  @Visitable
  private List<Node> methods;

  public JavaType(Kind kind, String name) {
    this.kind = kind;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Kind getKind() {
    return kind;
  }

  public void setKind(Kind kind) {
    this.kind = kind;
  }

  public List<Node> getFields() {
    return fields;
  }

  public List<Node> getMethods() {
    return methods;
  }
}
