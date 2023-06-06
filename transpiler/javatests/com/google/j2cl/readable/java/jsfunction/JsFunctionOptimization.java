/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jsfunction;

import jsinterop.annotations.JsFunction;

public class JsFunctionOptimization {

  @JsFunction
  interface F {
    String m(String s);
  }

  public void main(final int r) {
    new Object() {
      void m() {
        String x = "";
        new Object() {
          void m() {
            final int var = 1;
            F f =
                new F() {

                  @Override
                  public String m(String s) {
                    final int r1 = r;
                    final int var1 = var;
                    final String x1 = x;
                    return String.valueOf(r)
                        + s
                        + x
                        + var
                        + new F() {
                          @Override
                          public String m(String s) {
                            return s + r1 + x1 + var1;
                          }
                        }.m("hello");
                  }
                };
            F f2 =
                new F() {

                  @Override
                  public String m(String s) {
                    final int r1 = r;
                    final int var1 = var;
                    final String x1 = x;
                    return String.valueOf(r)
                        + s
                        + x
                        + var
                        + new Object() {
                          public String sayHey() {
                            return "Hey";
                          }
                        }.sayHey();
                  }
                };
          }
        };
      }
    };
  }

  @JsFunction
  interface Consumer {
    <T> void accept(T t);
  }

  public void testParameterizedMethod() {
    Consumer c =
        new Consumer() {
          @Override
          public <T> void accept(T t) {}
        };
  }
}
