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

/**
 * An enum for visibility.
 */
public enum Visibility {
  PUBLIC("public"),
  PROTECTED("protected"),
  PRIVATE("private"),
  PACKAGE_PRIVATE("package");

  public String value;

  Visibility(String value) {
    this.value = value;
  }

  public boolean isPublic() {
    return this == PUBLIC;
  }

  public boolean isProtected() {
    return this == PROTECTED;
  }

  public boolean isPrivate() {
    return this == PRIVATE;
  }

  public boolean isPackagePrivate() {
    return this == PACKAGE_PRIVATE;
  }

  @Override
  public String toString() {
    return value;
  }
}
