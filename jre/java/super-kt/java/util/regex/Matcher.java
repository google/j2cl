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
import jsinterop.annotations.JsNonNull;

/** JRE Matcher stub; see Matcher.kt for the actual wrapper. */
@KtNative("java.util.regex.Matcher")
public class Matcher {

  private Matcher() {}

  // Unsupported methods:
  // public native Matcher appendReplacement(StringBuffer sb, String replacement);
  // public native StringBuffer appendTail(StringBuffer sb);
  // public native MatchResult toMatchResult();
  // public native String replaceAll(String replacement);
  // public native String replaceFirst(String replacement);
  // public native boolean requiresEnd();
  // public native int groupCount();
  // public native Matcher useAnchoringBounds(boolean b);
  // public native Matcher useTransparentBounds(boolean b);
  // public native boolean lookingAt();
  // public native boolean hitEnd();

  public native int end();

  public native int end(int group);

  public native boolean find();

  public native boolean find(int start);

  public native String group();

  public native String group(int gorup);

  public native String group(String name);

  public native boolean hasAnchoringBounds();

  public native boolean hasTransparentBounds();

  public native boolean matches();

  public native Pattern pattern();

  public static native String quoteReplacement(String s);

  public native Matcher region(int start, int end);

  public native int regionEnd();

  public native int regionStart();

  public native Matcher reset();

  public native Matcher reset(CharSequence input);

  public native int start();

  public native int start(int group);

  public native String toString();

  public native Matcher usePattern(@JsNonNull Pattern newPattern);
}
