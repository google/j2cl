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
import third_party_java_src_j2cl_transpiler_javatests_com_google_j2cl_integration_java_j2ktiosinterop_SwiftInteropFramework

final class SwiftInteropTest: XCTestCase {
  func testDefaultNames() {
    var obj: J2ktiosinteropDefaultNames

    obj = J2ktiosinteropDefaultNames()
    obj = J2ktiosinteropDefaultNames(Int: 1)
    obj = J2ktiosinteropDefaultNames(Int: 1, withNSString: "")

    obj.method()
    obj.method(withBoolean: true)
    obj.method(withChar: 65)
    obj.method(withByte: 1)
    obj.method(withShort: 1)
    obj.method(withInt: 1)
    obj.method(withLong: 1)
    obj.method(withFloat: 1)
    obj.method(withDouble: 1)
    obj.method(withId: nil)
    obj.method(withNSString: nil)
    obj.method(withNSStringArray: nil)
    obj.method(withNSStringArray2: nil)
    obj.method(withNSCopying: nil)
    obj.method(withNSNumber: 1)
    obj.method(withIOSClass: nil)
    obj.method(withJavaLangIterable: nil)
    obj.method(withInt: 1, withNSString: nil)

    obj.genericMethod(withId: nil)
    obj.genericMethod(withNSString: nil)

    obj.field_ = obj.field_
    J2ktiosinteropDefaultNamesCompanion.shared.staticField_ =
      J2ktiosinteropDefaultNamesCompanion.shared.staticField_

    J2ktiosinteropDefaultNamesCompanion.shared.staticMethod()
    J2ktiosinteropDefaultNamesCompanion.shared.staticMethod(withInt: 1)
    J2ktiosinteropDefaultNamesCompanion.shared.staticMethod(withInt: 1, withNSString: "")
  }

  func testCustomNames() {
    var obj: Custom

    obj = Custom(Index: 1)
    obj = Custom(Index: 1, name: "")

    obj = Custom()
    obj = Custom(Long: 1)
    obj = Custom(Long: 1, withNSString: "")

    obj.custom()
    obj.custom(WithIndex: 1)
    obj.custom(WithIndex: 1, name: "")

    obj.custom(withLong: 1)
    obj.custom(withLong: 1, withNSString: "")

    CustomCompanion.shared.staticCustom()
    CustomCompanion.shared.staticCustom(WithIndex: 1)
    CustomCompanion.shared.staticCustom(WithIndex: 1, name: "")

    CustomCompanion.shared.staticCustom2(withLong: 1)
    CustomCompanion.shared.staticCustom3(withLong: 1, withNSString: "")
  }

  func testEnumNames() {
    var e: J2ktiosinteropEnumNames
    e = J2ktiosinteropEnumNames.ONE
    e = J2ktiosinteropEnumNames.TWO
  }
}
