/*
 * Copyright 2008 Google Inc.
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
package java.lang;

/**
 * NOTE: in GWT this is only thrown for division by zero on longs and BigInteger/BigDecimal.
 *
 * <p>See <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/ArithmeticException.html">the
 * official Java API doc</a> for details.
 */
public class ArithmeticException extends RuntimeException {
  public ArithmeticException(String explanation) {
    super(explanation);
  }

  public ArithmeticException() {
    super();
  }
}
