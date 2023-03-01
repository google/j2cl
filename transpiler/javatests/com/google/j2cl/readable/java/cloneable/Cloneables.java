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
package cloneable;

public class Cloneables {
  public static class WithoutCloneable {
    // Object.clone() is missing in J2CL JRE.
    // @Override
    public Object clone() throws CloneNotSupportedException {
      return new WithoutCloneable();
    }
  }

  public static class WithCloneable implements Cloneable {
    // Object.clone() is missing in J2CL JRE.
    // @Override
    public Object clone() throws CloneNotSupportedException {
      return new WithCloneable();
    }
  }

  public static final class WithoutCloneableChild extends WithoutCloneable {
    @Override
    public Object clone() throws CloneNotSupportedException {
      return new WithoutCloneableChild();
    }
  }

  public static final class WithCloneableChild extends WithCloneable {
    @Override
    public Object clone() throws CloneNotSupportedException {
      return new WithCloneableChild();
    }
  }
}
