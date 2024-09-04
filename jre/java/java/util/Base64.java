/*
 * Copyright 2024 Google Inc.
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

package java.util;

import java.nio.charset.StandardCharsets;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;

/** Partial J2cl emulation of Base64. */
public final class Base64 {
  private Base64() {}

  public static Base64.Decoder getDecoder() {
    return new Decoder();
  }

  public static Base64.Encoder getEncoder() {
    return new Encoder();
  }

  /** Partial J2cl Decoder emulation */
  public static class Decoder {
    public byte[] decode(String s) {
      return atob(s).getBytes(StandardCharsets.ISO_8859_1);
    }

    @JsMethod(namespace = JsPackage.GLOBAL)
    private static native String atob(String b);
  }

  /** Partial J2cl Encoder emulation */
  public static class Encoder {
    public String encodeToString(byte[] b) {
      return btoa(new String(b, StandardCharsets.ISO_8859_1));
    }

    @JsMethod(namespace = JsPackage.GLOBAL)
    private static native String btoa(String b);
  }
}
