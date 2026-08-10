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

/// Interop test for Swift.
final class SwiftInteropTest: XCTestCase {
  func testDefaultNames() {
    var obj: DefaultNames

    obj = DefaultNames()
    obj = DefaultNames(i: 1)
    obj = DefaultNames(i: 1, s: "")

    obj.method()
    obj.booleanMethod(c: true)
    obj.charMethod(c: 65)
    obj.byteMethod(b: 1)
    obj.shortMethod(s: 1)
    obj.intMethod(i: 1)
    obj.longMethod(l: 1)
    obj.floatMethod(f: 1.0)
    obj.doubleMethod(d: 1.0)
    obj.objectMethod(obj: nil)
    obj.stringMethod(obj: "")
    obj.stringArrayMethod(sa: nil)
    obj.stringArrayArrayMethod(saa: nil)
    obj.genericArrayMethod(t: nil)
    obj.genericStringArrayMethod(t: nil)
    obj.cloneableMethod(c: nil)
    obj.numberMethod(c: 1)
    obj.numberMethod(c: nil)
    obj.classMethod(c: nil)
    obj.stringIterableMethod(i: nil)
    obj.intStringMethod(i: 1, s: "")
    obj.customNamesMethod(c: nil)
    obj.defaultNamesMethod(c: nil)

    obj.genericMethod(t: nil)
    obj.genericStringMethod(t: nil)
    obj.genericStringAndComparableStringMethod(t: nil)
    obj.genericLongMethod(t: nil)
    obj.genericLongAndComparableLongMethod(t: nil)

    obj.overloadedMethod(o: nil)
    obj.overloadedMethod(i: 1)
    obj.overloadedMethod(l: 1)
    obj.overloadedMethod(f: 1.0)
    obj.overloadedMethod(d: 1.0)
    obj.overloadedMethod(s: "")

    DefaultNames.companion.staticMethod()

    // @Throws-annotated methods can throw in Swift and need to be wrapped in `try`
    // in J2kt. This causes a warning in J2Objc, where the annotation is not supported.
    // TODO(b/543354795): Hide these from Swift
    try! obj.throwsMethod()
    try! obj.throwsMethod(s: "")
    try! DefaultNames.companion.staticThrowsMethod()
    try! DefaultNames.companion.staticThrowsMethod(s: "")

    #if J2KT
      var i: Int32

      i = obj.finalIntField
      i = obj.intField
      obj.intField = i
    #else
      // Fields are not exposed to Swift in J2Objc.

      // TODO(b/543354795): Support in J2kt
      let _: Bool = obj.throwsMethodAndReturnError(error: nil)
      let _: Bool = DefaultNames.companion.staticThrowsMethodAndReturnError(error: nil)
    #endif
  }

  func testOnlyImplicitDefaultConstructor() {
    let _ = OnlyImplicitDefaultConstructor()
  }

  func testOnlyExplicitDefaultConstructor() {
    let _ = OnlyExplicitDefaultConstructor()
  }

  func testSpecialNames() {
    SpecialNames.WithBoolean().get(i: true)
    SpecialNames.WithChar().get(i: 65)
    SpecialNames.WithByte().get(i: 1)
    SpecialNames.WithShort().get(i: 1)
    SpecialNames.WithInt().get(i: 1)
    SpecialNames.WithLong().get(i: 1)
    SpecialNames.WithFloat().get(i: 1.0)
    SpecialNames.WithDouble().get(i: 1.0)
    SpecialNames.WithObject().get(i: nil)
    SpecialNames.WithString().get(i: "")
    SpecialNames.WithFoo().get(i: nil)
  }

  func testCustomNames() {
    var obj: CustomSwift

    obj = CustomSwift(index: 1)
    obj = CustomSwift(index: 1, name: "")

    obj = CustomSwift()
    obj.customMethod()

    #if J2KT
      obj = CustomSwift(long: 1)
      obj = CustomSwift(long: 1, with: "")
    #else
      obj = CustomSwift().init2(long: 1)
      obj = CustomSwift().init3(long: 1, with: "")
    #endif

    obj.customIntMethod(withInt: 1)
    obj.customIndexMethod(withIndex: 1)
    obj.customCountMethod(withCount: 1)
    obj.customStringMethod(withString: "")
    obj.customNameMethod(withName: "")
    obj.customIntStringMethod(withIndex: 1, name: "")

    obj.customLongMethod(withLong: 1)
    obj.customLongStringMethod(withLong: 1, with: "")

    obj.customCustomNamesMethod(with: nil)
    obj.customDefaultNamesMethod(with: nil)

    obj.customSwiftStringMethod(with: "")
    obj.objectiveCStringMethod(s: "")

    obj.lowercase("")

    CustomSwift.companion.customStaticMethod()
    CustomSwift.companion.customStaticIntMethod(withIndex: 1)
    CustomSwift.companion.customStaticIntStringMethod(withIndex: 1, name: "")
    CustomSwift.companion.customStaticLongMethod(withLong: 1)
    CustomSwift.companion.customStaticLongStringMethod(withLong: 1, with: "")
    CustomSwift.companion.staticlowercase("")
  }

  func testEnumNames() {
    let _ = EnumNames.one
    let _ = EnumNames.two

    let _ = EnumNamesEnum.one
    let _ = EnumNamesEnum.two

    #if J2KT
      let _ = J2ktiosinteropEnumNames_get_ONE()
      let _ = J2ktiosinteropEnumNames_get_TWO()

      let values = EnumNames.values()
      let _ = values.get(index: 0)
      let _ = values.get(index: 1)
    #else
      let _ = EnumNames.valueOf(name: "ONE")
      let _ = EnumNames.valueOf(name: "TWO")

      let values = EnumNames.values()!
      let _ = values[0]
      let _ = values[1]
    #endif
  }

  func testEnumComparison() {
    XCTAssertTrue(EnumNames.one === EnumNames.one)
    XCTAssertTrue(EnumNames.one !== EnumNames.two)

    XCTAssertTrue(EnumNames.one == EnumNames.one)
    XCTAssertTrue(EnumNames.one != EnumNames.two)
  }

  func testNativeDefaultName() {
    let obj = NativeDefaultName()
    let _ = obj.nativeInstanceMethod()
    let _ = NativeDefaultName.companion.nativeStaticMethod()
    let _ = NativeDefaultName.companion.nativeReturnType()
    NativeDefaultName.companion.nativeParameter(obj: obj)
  }

  func testNativeCustomName() {
    let obj = CustomNativeSwift()
    let _ = obj.nativeInstanceMethod()
    let _ = CustomNativeSwift.companion.nativeStaticMethod()
    let _ = CustomNativeSwift.companion.nativeReturnType()
    CustomNativeSwift.companion.nativeParameter(obj: obj)
  }

  func testPlatform() {
    #if J2KT
      XCTAssertEqual(J2ktiosinteropPlatform_get_NAME(), "J2KT")
    #else
      XCTAssertEqual(J2ktiosinteropPlatform_get_NAME(), "J2ObjC")
    #endif
  }

  func testNullability() {
    Nullability.companion.acceptNullable(v: nil)
    Nullability.companion.acceptNullableWithNonNullBound(v: nil)
    Nullability.companion.acceptWithNullableBound(v: nil)
    Nullability.companion.acceptNullableWithNullableBound(v: nil)

    J2ktiosinteropNullability_acceptNullableWithId_(nil)
    J2ktiosinteropNullability_acceptNullableWithNonNullBoundWithId_(nil)
    J2ktiosinteropNullability_acceptWithNullableBoundWithId_(nil)
    J2ktiosinteropNullability_acceptNullableWithNullableBoundWithId_(nil)
  }

  func testOverrides() {
    let parent = ObjectiveCNameOverrides.Parent()
    XCTAssertEqual(parent.parentWithOverride(), "parent")
    let child = ObjectiveCNameOverrides.Child()
    XCTAssertEqual(child.parentWithOverride(), "parent/child")
    XCTAssertEqual(child.childWithoutOverride(), "child")
  }

  func testDataClassRecord() {
    let record = DataClassRecord(a: 123, b: "foo")
    #if !J2KT
      // Infeasible in J2kt
      XCTAssertTrue(record is JavaLangRecord)
    #endif
    XCTAssertEqual(record.a, 123)
    XCTAssertEqual(record.b, "foo")
    XCTAssertTrue(record.description.contains("DataClassRecord"))
    XCTAssertTrue(record.description.contains("1"))
    XCTAssertTrue(record.description.contains("foo"))

    let record2 = DataClassRecord(a: 123, b: "foo")
    XCTAssertEqual(record, record2)
    XCTAssertEqual(record.hash, record2.hash)
  }
}
