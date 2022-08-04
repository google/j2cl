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

/** Pattern syntax error information. */
public class PatternSyntaxException extends IllegalArgumentException {

  private final String regex;
  private final int index;
  private final String description;

  public PatternSyntaxException(String desc, String regex, int index) {
    this.description = desc;
    this.regex = regex;
    this.index = index;
  }

  public int getIndex() {
    return index;
  }

  public String getRegex() {
    return regex;
  }

  public String getDescription() {
    return description;
  }
}
