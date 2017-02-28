package com.google.j2cl.transpiler.readable.multipleabstractparents;

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

  public static void main(String... args) {
    assert new ArrayList<String>().getFoo(null).equals("AbstractCollection");
    assert new StringList().getFoo(null).equals("AbstractCollection");
    assert ((IStringList) new StringList()).getFoo(null).equals("AbstractCollection");
  }
}
