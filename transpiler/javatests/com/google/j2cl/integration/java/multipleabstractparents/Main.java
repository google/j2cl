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
package multipleabstractparents;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  public interface List<T> {
    String getFoo(T t);
  }

  public abstract static class AbstractCollection<T> {
    @SuppressWarnings("unused")
    public String getFoo(T t) {
      return "AbstractCollection";
    }
  }

  public abstract static class AbstractList<T> extends AbstractCollection<T> implements List<T> {}

  public static class ArrayList<T> extends AbstractList<T> {}

  public interface IStringList extends List<String> {
    @Override
    public String getFoo(String string);
  }

  public abstract static class AbstractStringList extends AbstractList<String>
      implements IStringList {}

  public static class StringList extends AbstractStringList {}

  public static void main(String... args) {
    assertTrue(new ArrayList<String>().getFoo(null).equals("AbstractCollection"));
    assertTrue(new StringList().getFoo(null).equals("AbstractCollection"));
    assertTrue(((IStringList) new StringList()).getFoo(null).equals("AbstractCollection"));
  }
}
