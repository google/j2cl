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
package xplatforeach;

import com.google.apps.docs.xplat.collections.JsArrayInteger;
import com.google.apps.docs.xplat.collections.SerializedJsArray;
import com.google.apps.docs.xplat.collections.SerializedJsMap;
import com.google.apps.docs.xplat.collections.UnsafeJsMap;
import com.google.apps.docs.xplat.collections.UnsafeJsMapInteger;
import com.google.apps.docs.xplat.collections.UnsafeJsSet;
import com.google.apps.docs.xplat.structs.SparseArray;
import com.google.gwt.corp.collections.ImmutableJsArray;
import com.google.gwt.corp.collections.JsArray;
import com.google.gwt.corp.collections.UnmodifiableJsArray;

public class ForEachStatements {
  static void jsArray(JsArray<Throwable> array) {
    for (Throwable element : array.getIterable()) {
      String.valueOf(element);
    }
  }

  static void jsArrayOfJsEnum(JsArray<SomeJsEnum> array) {
    for (SomeJsEnum element : array.getIterable()) {
      String.valueOf(element.value);
    }
  }

  static void jsArrayOfNativeJsEnum(JsArray<NativeJsEnum> array) {
    for (NativeJsEnum element : array.getIterable()) {
      String.valueOf(element);
    }
  }

  static void immutableJsArray(ImmutableJsArray<Throwable> array) {
    for (Throwable element : array.getIterable()) {
      String.valueOf(element);
    }
  }

  static void unmodifiableJsArray(UnmodifiableJsArray<Throwable> array) {
    for (Throwable element : array.getIterable()) {
      String.valueOf(element);
    }
  }

  static void jsArrayInteger(JsArrayInteger array) {
    for (int element : array.getIterable()) {
      String.valueOf(element);
    }

    for (Integer element : array.getIterable()) {
      String.valueOf(element);
    }
  }

  static void serializedJsArray(SerializedJsArray array) {
    for (Object element : array.getIterable()) {
      String.valueOf(element);
    }
  }

  static void serializedJsMap(SerializedJsMap map) {
    String keys = "";
    for (String key : map.getIterableKeys()) {
      keys += key;
    }
  }

  static void unsafeJsMap(UnsafeJsMap<?> map) {
    String keys = "";
    for (String key : map.getIterableKeys()) {
      keys += key;
    }
  }

  static void unsafeJsMapInteger(UnsafeJsMapInteger map) {
    String keys = "";
    for (String key : map.getIterableKeys()) {
      keys += key;
    }
  }

  static void unsafeJsSet(UnsafeJsSet set) {
    String keys = "";
    for (String key : set.getIterableKeys()) {
      keys += key;
    }
  }

  static void sparseArray(SparseArray<?> array) {
    int sumOfKeys = 0;
    for (Integer key : array.getIterableKeys()) {
      sumOfKeys += key;
    }

    for (int key : array.getIterableKeys()) {
      sumOfKeys += key;
    }
  }
}
