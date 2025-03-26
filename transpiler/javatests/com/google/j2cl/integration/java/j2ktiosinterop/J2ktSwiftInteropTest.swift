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
import XCTest
import third_party_java_src_j2cl_transpiler_javatests_com_google_j2cl_integration_java_j2ktiosinterop_J2ktSwiftInteropFramework

/// J2KT interop test for Swift.
final class J2ktSwiftInteropTest: XCTestCase {
  func testDefaultNames() {
    var obj: J2ktiosinteropDefaultNames

    obj = J2ktiosinteropDefaultNames()
    // TODO(b/374280337): Should be J2ktiosinteropDefaultNames(int:)
    obj = J2ktiosinteropDefaultNames(Int: 1)
    // TODO(b/374280337): Should be J2ktiosinteropDefaultNames(int:with:)
    obj = J2ktiosinteropDefaultNames(Int: 1, withNSString: "")

    obj.method()
    obj.booleanMethod(withBoolean: true)
    obj.charMethod(withChar: 65)
    obj.byteMethod(withByte: 1)
    obj.shortMethod(withShort: 1)
    obj.intMethod(with: 1)
    obj.longMethod(withLong: 1)
    obj.floatMethod(with: 1)
    obj.doubleMethod(with: 1)
    obj.objectMethod(withId: nil)
    obj.stringMethod(with: "")
    obj.stringArrayMethod(withNSStringArray: nil)
    obj.stringArrayArrayMethod(withNSStringArray2: nil)
    obj.cloneableMethod(with: nil)
    obj.numberMethod(with: 1)
    obj.classMethod(with: nil)
    obj.stringIterableMethod(with: nil)
    obj.intStringMethod(with: 1, with: "")

    obj.genericMethod(withId: nil)
    obj.genericStringMethod(with: "")

    // TODO(b/374280337): Should be obj.getWith(1)
    obj.get(with: 1)

    obj.intField_ = obj.intField_
    J2ktiosinteropDefaultNamesCompanion.shared.staticIntField_ =
      J2ktiosinteropDefaultNamesCompanion.shared.staticIntField_

    J2ktiosinteropDefaultNamesCompanion.shared.staticMethod()
    J2ktiosinteropDefaultNamesCompanion.shared.staticIntMethod(with: 1)
    J2ktiosinteropDefaultNamesCompanion.shared.staticIntStringMethod(with: 1, with: "")
  }

  func testCustomNames() {
    var obj: Custom

    obj = Custom(Index: 1)
    obj = Custom(Index: 1, name: "")

    obj = Custom()
    obj = Custom(Long: 1)
    obj = Custom(Long: 1, withNSString: "")

    obj.customMethod()
    obj.customIntMethod(WithInt: 1)
    obj.customIndexMethod(WithIndex: 1)
    obj.customCountMethod(WithCount: 1)
    obj.customStringMethod(WithString: "")
    obj.customNameMethod(WithName: "")
    obj.customIntStringMethod(WithIndex: 1, name: "")

    obj.customLongMethod(withLong: 1)
    obj.customLongStringMethod(withLong: 1, with: "")

    CustomCompanion.shared.customStaticMethod()
    CustomCompanion.shared.customStaticIntMethod(WithIndex: 1)
    CustomCompanion.shared.customStaticIntStringMethod(WithIndex: 1, name: "")

    CustomCompanion.shared.customStaticLongMethod(withLong: 1)
    CustomCompanion.shared.customStaticLongStringMethod(withLong: 1, with: "")
  }

  func testEnumNames() {
    let _ = J2ktiosinteropEnumNames.ONE
    let _ = J2ktiosinteropEnumNames.TWO
  }
}
