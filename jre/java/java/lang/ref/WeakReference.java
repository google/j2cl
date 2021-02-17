package java.lang.ref;

public class WeakReference<T> {

  private T referent;

  public WeakReference(T referent) {
    this.referent = referent;
  }

  public T get() {
    return referent;
  }

  public void clear() {
    referent = null;
  }
}
