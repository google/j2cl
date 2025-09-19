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
import third_party_java_src_j2cl_transpiler_javatests_com_google_j2cl_integration_java_j2ktiosinterop_j2objc

/// J2ObjC interop test for Swift.
final class J2ObjCSwiftInteropTest: XCTestCase {
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
    obj.floatMethod(with: 1.0)
    obj.doubleMethod(with: 1.0)
    obj.objectMethod(withId: nil)
    obj.stringMethod(with: "")
    obj.stringArrayMethod(withNSStringArray: nil)
    obj.stringArrayArrayMethod(withNSStringArray2: nil)
    obj.genericArrayMethod(withNSObjectArray: nil)
    obj.genericStringArrayMethod(withNSStringArray: nil)
    obj.cloneableMethod(with: nil)
    obj.numberMethod(with: nil)
    obj.classMethod(with: nil)
    obj.stringIterableMethod(with: nil)
    obj.intStringMethod(with: 1, with: "")
    obj.customNamesMethod(with: nil)
    obj.defaultNamesMethod(with: nil)

    obj.genericMethod(withId: nil)
    obj.genericStringMethod(with: "")

    obj.overloadedMethod(withId: nil)
    obj.overloadedMethod(with: 1 as jint)
    obj.overloadedMethod(withLong: 1 as jlong)

    obj.overloadedMethod(with: 1 as jfloat)
    obj.overloadedMethod(with: 1 as jdouble)
    obj.overloadedMethod(with: "")

    // Fields are not exposed in Swift
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

    obj = Custom(index: 1)
    obj = Custom(index: 1, name: "")

    obj = Custom()
    // Not exposed on Swift
    //obj = Custom(long: 1)
    //obj = Custom(long: 1, withNSString: "")

    obj.customIntMethod(with: 1)
    obj.customIndexMethod(with: 1)
    obj.customCountMethod(withCount: 1)
    obj.customStringMethod(with: "")
    obj.customNameMethod(withName: "")
    obj.customIntStringMethod(with: 1, name: "")

    obj.customLongMethod(withLong: 1)
    obj.customLongStringMethod(withLong: 1, with: "")

    obj.customCustomNamesMethod(with: nil)
    obj.customDefaultNamesMethod(with: nil)

    obj.customObjectiveCSwiftStringMethod(with: "")
    obj.customSwiftStringMethod(with: "")

    Custom_customStaticMethod()
    Custom_customStaticIntMethodWithIndex_(1)
    Custom_customStaticIntStringMethodWithIndex_name_(1, "")
    Custom_customStaticLongMethod(1)
    Custom_customStaticLongStringMethod(2, "")

    obj.lowercase("")
    Custom_staticlowercase_("")
  }

  func testEnumNames() {
    let _ = J2ktiosinteropEnumNames_get_ONE()
    let _ = J2ktiosinteropEnumNames_get_TWO()

    // Not exposed in Swift
    // let _ = J2ktiosinteropEnumNames_fromOrdinal(J2ktiosinteropEnumNames_Enum_ONE)
    // let _ = J2ktiosinteropEnumNames_fromOrdinal(J2ktiosinteropEnumNames_Enum_TWO)

    // let _ = J2ktiosinteropEnumNames_Enum_ONE
    // let _ = J2ktiosinteropEnumNames_Enum_TWO

    let _ = J2ktiosinteropEnumNames.valueOf(with: "ONE")
    let _ = J2ktiosinteropEnumNames.valueOf(with: "TWO")

    let values = J2ktiosinteropEnumNames.values()!
    let _ = values[0]
    let _ = values[1]
  }
}
