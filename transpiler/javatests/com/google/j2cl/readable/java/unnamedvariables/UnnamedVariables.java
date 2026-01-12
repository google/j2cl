/*
 * Copyright 2026 Google Inc.
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

package unnamedvariables;

import java.util.List;
import java.util.function.Function;

public class UnnamedVariables {

  void testSwitchPatterns() {
    record R(Object o, String s) {}
    Object r = null;
    switch (r) {
      // A binding pattern with an unnamed variable, only the instanceof check is needed but no
      // assignment to a temporary variable.
      case String _ -> {}
      // A record pattern with two unnamed variables. The second one is unconditional, so no
      // code should be generated for it.
      case R(String _, String _) -> {}
      // A record pattern with an unnamed variable and an AnyPattern. No code should be generated
      // for the AnyPattern, but an instanceof check is needed for the first unnamed variable since
      // the pattern there is not unconditional.
      case R(Number _, _) -> {}
      // A record pattern with all its components being AnyPatterns. Only the instanceof check
      // for the record type is needed but not the assignment to a temporary variable.
      case R(_, _) -> {}
      default -> {}
    }
  }

  void testInstanceOfPatterns() {
    record R(Object o, String s) {}
    Object r = null;
    var _ = r instanceof String _;
    var _ = r instanceof R(String _, String _);
    var _ = r instanceof R(Number _, _);
    var _ = r instanceof R(_, _);
  }

  void testDeclaration() {
    // The statement should be removed because the variable is unnamed and the expression does not
    // have side effects.
    var _ = "Hello";
    // The variable declaration should be removed but the initializer should be kept.
    var _ = "Hello".length();
    // The variable declaration should be removed but the initializer should be kept.
    int _ = "Bye".length();
  }

  void testLambda() {
    Function<String, String> f1 = _ -> "Hello";
    Function<String, String> f2 = (String _) -> "Hello";
  }

  void testTryCatch() {
    AutoCloseable closable = null;
    try (var _ = closable) {
    } catch (RuntimeException _) {
      // Empty catch block.
    } catch (Exception | Error _) {
      // Empty catch block.
    }
  }

  void testForEach() {
    String[] a = null;
    for (var _ : a) {}
    for (String _ : a) {}

    List<String> l = null;
    for (var _ : l) {}
    for (String _ : l) {}
  }
}
