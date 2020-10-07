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
package multipleconstructors;

public class MultipleConstructors {
  private int id;
  private boolean flag;

  public MultipleConstructors(int id) {
    this.id = id;
    this.flag = (id == 0);
  }

  public MultipleConstructors(boolean flag) {
    this.id = -1;
    this.flag = flag;
  }

  public MultipleConstructors(int id, boolean flag) {
    this.id = id;
    this.flag = flag;
  }
}
