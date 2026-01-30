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
import third_party_java_src_j2cl_transpiler_javatests_com_google_j2cl_integration_java_j2ktiosinterop_j2ktiosinterop_j2objc

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
    obj.customNamesMethod(with: nil)
    obj.defaultNamesMethod(with: nil)

    obj.genericMethod(withId: nil)
    obj.genericStringMethod(with: nil)
    obj.genericStringAndComparableStringMethod(with: nil)
    obj.genericLongMethod(with: nil)
    obj.genericLongAndComparableLongMethod(with: nil)

    obj.overloadedMethod(withId: nil)
    obj.overloadedMethod(with: 1 as Int32)
    obj.overloadedMethod(withLong: 1 as Int64)

    obj.overloadedMethod(with: 1 as Float)
    obj.overloadedMethod(with: 1 as Double)
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

    // @Throws-annotated methods (supposedly) throw in Swift and need to be wrapped in `try!`.
    try! obj.throwsMethod()
    try! obj.throwsMethod(with: "")
    // Unsupported because of b/441110909.
    // try! J2ktiosinteropDefaultNamesCompanion.shared.staticThrowsMethod()
    // try! J2ktiosinteropDefaultNamesCompanion.shared.staticThrowsMethod(with: "")
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
    obj = Custom(long: 1)
    obj = Custom(long: 1, with: "")

    obj.customMethod()
    obj.customIntMethod(WithInt: 1)
    obj.customIndexMethod(WithIndex: 1)
    obj.customCountMethod(WithCount: 1)
    obj.customStringMethod(WithString: "")
    obj.customNameMethod(WithName: "")
    obj.customIntStringMethod(WithIndex: 1, name: "")

    obj.customLongMethod(withLong: 1)
    obj.customLongStringMethod(withLong: 1, with: "")

    obj.customCustomNamesMethod(with: nil)
    obj.customDefaultNamesMethod(with: nil)

    obj.customObjectiveCSwiftStringMethod(with: "")
    obj.customSwiftStringMethod(with: "")

    Custom.Companion.shared.customStaticMethod()
    Custom.Companion.shared.customStaticIntMethod(WithIndex: 1)
    Custom.Companion.shared.customStaticIntStringMethod(WithIndex: 1, name: "")

    Custom.Companion.shared.customStaticLongMethod(withLong: 1)
    Custom.Companion.shared.customStaticLongStringMethod(withLong: 1, with: "")

    // TODO(b/441689301): Unsupported because of https://youtrack.jetbrains.com/issue/KT-80557
    // obj.lowercase("")
    // Custom.Companion.shared.staticlowercase("")
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

  func testEnumComparison() {
    XCTAssertTrue(J2ktiosinteropEnumNames_get_ONE() === J2ktiosinteropEnumNames_get_ONE())
    XCTAssertTrue(J2ktiosinteropEnumNames_get_ONE() !== J2ktiosinteropEnumNames_get_TWO())

    XCTAssertTrue(J2ktiosinteropEnumNames_get_ONE() == J2ktiosinteropEnumNames_get_ONE())
    XCTAssertTrue(J2ktiosinteropEnumNames_get_ONE() != J2ktiosinteropEnumNames_get_TWO())

    // Kotlin Enum is not Comparable in Objective-C.
    // XCTAssertEqual(
    //   J2ktiosinteropEnumNames_get_ONE().compareTo(withId: J2ktiosinteropEnumNames_get_ONE()), 0)
    // XCTAssertEqual(
    //   J2ktiosinteropEnumNames_get_ONE().compareTo(withId: J2ktiosinteropEnumNames_get_TWO()), -1)
    // XCTAssertEqual(
    //   J2ktiosinteropEnumNames_get_TWO().compareTo(withId: J2ktiosinteropEnumNames_get_ONE()), 1)
  }

  func testNativeDefaultName() {
    let obj = J2ktiosinteropNativeDefaultName()
    let _ = obj.nativeInstanceMethod()
    let _ = J2ktiosinteropNativeDefaultName.Companion.shared.nativeStaticMethod()
    J2ktiosinteropNativeDefaultName.Companion.shared.nativeParameter(with: obj)
    let _ = J2ktiosinteropNativeDefaultName.Companion.shared.nativeReturnType()
  }

  func testNativeCustomName() {
    // Unsupported because of b/441110909.
    // let obj = CustomNativeClass()
    // let _ = obj.nativeInstanceMethod()
    // let _ = CustomNativeClass.Companion.shared.nativeStaticMethod()
    // CustomNativeClass.Companion.shared.nativeParameter(with: obj)
    // let _ = CustomNativeClass.Companion.shared.nativeReturnType()
  }

  func testPlatform() {
    XCTAssertEqual(J2ktiosinteropPlatform_get_NAME(), "J2KT")
  }

  func testNullability() {
    J2ktiosinteropNullability_acceptNullableWithId_(nil)
    J2ktiosinteropNullability_acceptNullableWithNonNullBoundWithId_(nil)
    // TODO(b/460155951): Uncomment when fixed.
    // J2ktiosinteropNullability_acceptWithNullableBoundWithId_(nil);
    J2ktiosinteropNullability_acceptNullableWithNullableBoundWithId_(nil)
  }
}
