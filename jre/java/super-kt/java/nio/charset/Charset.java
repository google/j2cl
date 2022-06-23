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
package java.nio.charset;

/** Partial emulation of the corresponding JRE-Class */
public class Charset {

  private String canonicalName;

  protected Charset(String canonicalName, String[] aliases) {
    this.canonicalName = canonicalName;
  }

  public String name() {
    return canonicalName;
  }

  public static Charset forName(String name)
      throws UnsupportedCharsetException, IllegalArgumentException {
    if (name == null) {
      throw new IllegalArgumentException();
    }
    switch (name) {
      case "UTF-8":
        return StandardCharsets.UTF_8;
      default:
        throw new UnsupportedCharsetException(name);
    }
  }
}
