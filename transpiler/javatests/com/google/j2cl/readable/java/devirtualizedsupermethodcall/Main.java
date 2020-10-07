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
package devirtualizedsupermethodcall;

class SubNumber extends Number {
  @Override
  public int intValue() {
    return 0;
  }

  @Override
  public long longValue() {
    return (long) 0;
  }

  @Override
  public float floatValue() {
    return 0;
  }

  @Override
  public double doubleValue() {
    return 0;
  }
}

class FooCallsSuperObjectMethod {
  @Override
  public int hashCode() {
    return super.hashCode();
  }
}

public class Main {
  public void main() {
    FooCallsSuperObjectMethod fooCallsSuperObjectMethods = new FooCallsSuperObjectMethod();
    fooCallsSuperObjectMethods.hashCode();

    SubNumber sn = new SubNumber();
    sn.byteValue();
    sn.doubleValue();
    sn.floatValue();
    sn.intValue();
    sn.shortValue();
  }
}
