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
    obj = J2ktiosinteropDefaultNames(int: 1)
    obj = J2ktiosinteropDefaultNames(int: 1, with: "")

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
    obj.genericArrayMethod(withNSObjectArray: nil)
    obj.genericStringArrayMethod(withNSStringArray: nil)
    obj.cloneableMethod(with: nil)
    obj.numberMethod(with: 1)
    obj.classMethod(with: nil)
    obj.stringIterableMethod(with: nil)
    obj.intStringMethod(with: 1, with: "")

    obj.genericMethod(withId: nil)
    obj.genericStringMethod(with: "")

    obj.overloadedMethod(withId: nil)
    obj.overloadedMethod(with: 1 as jint)
    obj.overloadedMethod(withLong: 1 as jlong)

    obj.overloadedMethod(with: 1 as jfloat)
    obj.overloadedMethod(with: 1 as jdouble)
    obj.overloadedMethod(with: "")

    var i: Int32

    i = obj.finalIntField_
    i = obj.intField_
    obj.intField_ = i

    // Unsupported because of b/441110909.
    // i = J2ktiosinteropDefaultNamesCompanion.shared.STATIC_FINAL_INT_FIELD_
    // i = J2ktiosinteropDefaultNamesCompanion.shared.staticIntField_
    // J2ktiosinteropDefaultNamesCompanion.shared.staticIntField_ = i

    // J2ktiosinteropDefaultNamesCompanion.shared.staticMethod()
    // J2ktiosinteropDefaultNamesCompanion.shared.staticIntMethod(with: 1)
    // J2ktiosinteropDefaultNamesCompanion.shared.staticIntStringMethod(with: 1, with: "")
  }

  func testOnlyImplicitDefaultConstructor() {
    let _ = J2ktiosinteropOnlyImplicitDefaultConstructor()
  }

  func testOnlyExplicitDefaultConstructor() {
    let _ = J2ktiosinteropOnlyExplicitDefaultConstructor()
  }

  func testSpecialNames() {
    J2ktiosinteropSpecialNames_WithBoolean().getWithBoolean(true)
    J2ktiosinteropSpecialNames_WithChar().getWithChar(65)
    J2ktiosinteropSpecialNames_WithByte().getWithByte(1)
    J2ktiosinteropSpecialNames_WithShort().getWithShort(1)
    J2ktiosinteropSpecialNames_WithInt().getWith(1)
    J2ktiosinteropSpecialNames_WithLong().getWithLong(1)
    J2ktiosinteropSpecialNames_WithFloat().getWith(1.0)
    J2ktiosinteropSpecialNames_WithDouble().getWith(1.0)
    J2ktiosinteropSpecialNames_WithObject().getWithId(nil)
    J2ktiosinteropSpecialNames_WithString().getWith("")
    J2ktiosinteropSpecialNames_WithFoo().getWith(nil)
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

    // TODO(b/441732853): Should be customObjectiveCSwiftStringMethod(with: "")
    obj.customObjectiveCSwiftStringMethod(WithString: "")
    obj.customSwiftStringMethod(with: "")

    CustomCompanion.shared.customStaticMethod()
    CustomCompanion.shared.customStaticIntMethod(WithIndex: 1)
    CustomCompanion.shared.customStaticIntStringMethod(WithIndex: 1, name: "")

    CustomCompanion.shared.customStaticLongMethod(withLong: 1)
    CustomCompanion.shared.customStaticLongStringMethod(withLong: 1, with: "")

    // TODO(b/441689301): Unsupported because of https://youtrack.jetbrains.com/issue/KT-80557
    // obj.lowercase("")
    // CustomCompanion.shared.staticlowercase("")
  }

  func testEnumNames() {
    let _ = J2ktiosinteropEnumNames_get_ONE()
    let _ = J2ktiosinteropEnumNames_get_TWO()

    // Not exposed in Swift
    // let _ = J2ktiosinteropEnumNames_fromOrdinal(J2ktiosinteropEnumNames_Enum_ONE)
    // let _ = J2ktiosinteropEnumNames_fromOrdinal(J2ktiosinteropEnumNames_Enum_TWO)

    // let _ = J2ktiosinteropEnumNames_Enum_ONE
    // let _ = J2ktiosinteropEnumNames_Enum_TWO

    // let _ = J2ktiosinteropEnumNames.valueOf(with:"ONE")
    // let _ = J2ktiosinteropEnumNames.valueOf(with:"TWO")

    let values = J2ktiosinteropEnumNames.values()
    let _ = values.get(index: 0)
    let _ = values.get(index: 1)
  }
}
