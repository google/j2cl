package com.google.j2cl.transpiler.readable.multipleabstractparents;

public class MultipleAbstractParents {
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
    assert new ArrayList<String>().getFoo(null).equals("AbstractCollection");
    assert new StringList().getFoo(null).equals("AbstractCollection");

    // TODO: restore when the missing "m_getFoo__java_lang_String()" bridge in "AbstractStringList"
    // is fixed.
    // assert ((IStringList) new StringList()).getFoo(null).equals("AbstractCollection");
  }
}
