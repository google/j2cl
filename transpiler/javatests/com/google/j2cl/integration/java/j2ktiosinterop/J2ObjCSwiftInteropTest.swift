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
    obj.method(withBoolean: true as jboolean)
    obj.method(withChar: 65 as jchar)
    obj.method(withByte: 1 as jbyte)
    obj.method(withShort: 1 as jshort)
    obj.method(with:1 as jint)
    obj.method(withLong: 1 as jlong)
    obj.method(with:1.0 as jfloat)
    obj.method(with:1.0 as jdouble)
    obj.method(withId: nil)
    obj.method(with: nil as String?)
    obj.method(withNSStringArray: nil)
    obj.method(withNSStringArray2: nil)
    //obj.methodWith(nil as NSCopying?)
    obj.method(with: 1 as NSNumber)
    obj.method(with: nil as IOSClass?)
    obj.method(with: nil as JavaLangIterable?)
    obj.method(with: 1, with: nil)

    obj.genericMethod(withId: nil as Any?)
    obj.genericMethod(with: nil as String?)

    // Not exposed in Swift
    //obj.field_ = obj.field_
  }

  func testCustomNames() {
    var obj: Custom

    obj = Custom(index: 1 as jint)
    obj = Custom(index: 1 as jint, name: "")

    obj = Custom()
    // Not exposed on Swift
    //obj = Custom(long: 1 as jlong)
    //obj = Custom(long: 1 as jlong, withNSString: "")

    obj.custom()
    obj.custom(with: 1 as jint)
    obj.custom(with: 1 as jint, name: "")

    obj.custom(withLong: 1 as jlong)
    obj.custom(withLong: 1 as jlong, with: "")
  }

  func testEnumNames() {
    var e: J2ktiosinteropEnumNames
    e = J2ktiosinteropEnumNames_get_ONE()
    e = J2ktiosinteropEnumNames_get_TWO()

    var e2: J2ktiosinteropEnumNames_Enum
    // Not exposed on Swift
    //e2 = J2ktiosinteropEnumNames_Enum_ONE;
    //e2 = J2ktiosinteropEnumNames_Enum_TWO;
  }
}
