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
package jstypeswithconstructor;

import jsinterop.annotations.JsType;

/**
 * Currently this class is used to test that no bridge method is created for constructor.
 *
 * <p>The output may be changed when we generate exported JsConstructor properly.
 */
@JsType
public class JsTypesWithConstructor {
  public JsTypesWithConstructor() {}
}
