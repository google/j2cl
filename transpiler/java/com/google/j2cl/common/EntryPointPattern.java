/*
 * Copyright 2023 Google Inc.
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
package com.google.j2cl.common;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import java.util.regex.Pattern;

/** Pattern that describes a set of methods that are entry points. */
@AutoValue
public abstract class EntryPointPattern {

  public abstract String getEntryPointPatternString();

  public boolean matchesClass(String qualifiedClassName) {
    return getEnclosingClassPattern().matcher(qualifiedClassName).matches();
  }

  public boolean matchesMethod(String qualifiedClassName, String methodName) {
    return getEnclosingClassPattern().matcher(qualifiedClassName).matches()
        && getMethodPattern().matcher(methodName).matches();
  }

  /** Builds an EntryPointPattern from a string. */
  public static EntryPointPattern from(String string) {
    return new AutoValue_EntryPointPattern(string);
  }

  /**
   * Regular expression that only allow valid identifier characters, the package separator '.' and
   * the regular expression '.*'.
   */
  private static final String QUALIFIED_NAME_VALIDATION_REGEX = "([\\w$_.]|(\\.\\*))+";
  /**
   * Regular expression that only allow valid identifier characters and the regular expression '.*'.
   */
  private static final String METHOD_NAME_VALIDATION_REGEX = "([\\w$_]|(\\.\\*))+";

  public boolean isValid() {
    return getEntryPointPatternString()
        .matches(QUALIFIED_NAME_VALIDATION_REGEX + "#" + METHOD_NAME_VALIDATION_REGEX);
  }

  @Memoized
  Pattern getEnclosingClassPattern() {
    String entryPointClassString =
        getEntryPointPatternString().substring(0, getEntryPointPatternString().lastIndexOf('#'));
    return getEntryPointAsPattern(entryPointClassString);
  }

  @Memoized
  Pattern getMethodPattern() {
    String entryPointMethodString =
        getEntryPointPatternString().substring(getEntryPointPatternString().lastIndexOf('#') + 1);
    return getEntryPointAsPattern(entryPointMethodString);
  }

  private static Pattern getEntryPointAsPattern(String entryPointString) {
    // Convert the entry point expression semantics into a Java regex. Entry point expression
    // semantics only allows the regex '.*', so we first escape all '.', and then unescape the
    // accidentally escaped '.' that were part of a '.*'.
    String simpleRegEx = entryPointString.replace(".", "\\.").replace("\\.*", ".*");
    return Pattern.compile(simpleRegEx);
  }
}
