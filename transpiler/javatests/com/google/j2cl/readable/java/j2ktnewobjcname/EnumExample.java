/*
 * Copyright 2023 Google Inc.
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
package j2ktnewobjcname;

public enum EnumExample {
  SINGLEPART,
  TWO_PARTS,
  EVEN_MORE_PARTS,

  singlepart,
  twoParts,
  evenMoreParts,

  single_part,
  two_parts,
  even_more_parts,

  X_Y_Z,
  xyz,
  x_y_z,

  VEC_X_Y_Z,
  VEC_XYZ,
  vecXyz,
  vecXYZ,

  val,
  var,
  fun,
  alloc,
  init,
  initialize,
  allocFoo,
  initFoo,
  newFoo,
  copyFoo,
  mutableCopyFoo,
  register,
  struct,
  NULL,
  YES,
  NO;

  public static EnumExample withOrdinal(int ordinal) {
    return EnumExample.values()[ordinal];
  }
}
