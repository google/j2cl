/*
 * Copyright 2022 Google Inc.
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
package java.util.regex;

import javaemul.internal.annotations.KtNative;

/** JRE Pattern class stub in Javadoc order. The actual wrapper is in the parallel Kotlin class. */
@KtNative("java.util.regex.Pattern")
public class Pattern {

  public static /* final */ int CANON_EQ;
  public static /* final */ int CASE_INSENSITIVE;
  public static /* final */ int COMMENTS;
  public static /* final */ int DOTALL;
  public static /* final */ int LITERAL;
  public static /* final */ int MULTILINE;
  public static /* final */ int UNICODE_CASE;
  public static /* final */ int UNICODE_CHARACTER_CLASS;
  public static /* final */ int UNIX_LINES;

  public static native Pattern compile(String regex);

  public static native Pattern compile(String regex, int flags);

  public native int flags();

  public native Matcher matcher(CharSequence input);

  public static native boolean matches(String regex, CharSequence input);

  public static native String pattern();

  public static native String quote(String s);

  public native String[] split(CharSequence input);

  public native String[] split(CharSequence input, int limit);

  public native String toString();

  private Pattern() {}
}
