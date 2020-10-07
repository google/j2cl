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

public class MultipleAbstractParents {
  public interface List<T> {
    String getFoo(T t);
  }

  public interface Collection<T> {
    String getFoo(T t);
  }

  public abstract static class AbstractListCollection<T> implements List<T>, Collection<T> {}

  public abstract static class AbstractCollection<T> {
    @SuppressWarnings("unused")
    public String getFoo(T t) {
      return "AbstractCollection";
    }
  }

  public abstract static class AbstractList<T> extends AbstractCollection<T> implements List<T> {}

  public abstract static class AbstractList2<T> implements List<T> {}

  public static class ArrayList<T> extends AbstractList<T> {}

  public interface IStringList extends List<String> {
    @Override
    public String getFoo(String string);
  }

  public abstract static class AbstractStringList extends AbstractList<String>
      implements IStringList {}

  public abstract static class AbstractStringList2 extends AbstractList2<String>
      implements IStringList {}

  public abstract static class AbstractStringList3 extends AbstractList2<String> {}

  public static class StringList extends AbstractStringList {}

  public static class StringListChild extends StringList {}

  public static void main(String... args) {
    assert new ArrayList<String>().getFoo(null).equals("AbstractCollection");
    assert new StringList().getFoo(null).equals("AbstractCollection");
    assert ((IStringList) new StringList()).getFoo(null).equals("AbstractCollection");
  }
}
