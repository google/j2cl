package java.lang.ref;

import jsinterop.annotations.JsType;

import static jsinterop.annotations.JsPackage.GLOBAL;

public class WeakReference<T> {

  @JsType(isNative = true, name = "WeakRef", namespace = GLOBAL)
  static class JsWeakRef<T> {
    public JsWeakRef(T referent) {
    }

    public native T deref();
  }

  private JsWeakRef<T> referent;

  public WeakReference(T referent) {
    this.referent = new JsWeakRef<>(referent);
  }

  public T get() {
    return referent != null ? referent.deref() : null;
  }

  public void clear() {
    referent = null;
  }
}
