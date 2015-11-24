package java.lang;

import jsinterop.annotations.JsMethod;

/**
 * See <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/String.html">the
 * official Java API doc</a> for details.
 * TODO: implements Comparable, CharSequence
 */
public final class String implements java.io.Serializable, Comparable<String>, CharSequence {

  // Object overrides

  @Override
  public boolean equals(Object o) {
    return this == o;
  }

  @Override
  public int hashCode() {
    return $hashCode(this);
  }

  @Override
  public String toString() {
    return this;
  }

  // Char Sequence Implementation

  public int length() {
    return $length(this);
  }

  public char charAt(int index) {
    return $charAt(this, index);
  }

  public CharSequence subSequence(int start, int end) {
    return $subSequence(this, start, end);
  }

  // Comparable Implementation

  public int compareTo(String s) {
    return $compareTo(this, s);
  }

  // (Subset of) Public Methods Implementation

  public static String valueOf(Object o) {
    return o == null ? "null" : o.toString();
  }

  public String substring(int start, int endIndex) {
    return $substring(this, start, endIndex);
  }

  public String substring(int start) {
    return $substring(this, start);
  }

  public String trim() {
    return $trim(this);
  }

  @JsMethod
  private static native boolean nativeIsInstance(
      Object instance) /*-{return typeof instance == 'string';}-*/;

  @JsMethod
  private static native int $hashCode(String obj) /*-{
    let hashCode = 0;
    let len = obj.length;
    for (let i = 0; i < len; i++) {
      hashCode += obj.charCodeAt(i) * Math.pow(31, len - i - 1);
    }
    return hashCode;
  }-*/;

  @JsMethod
  private static native int $length(String obj) /*-{ return obj.length; }-*/;

  @JsMethod
  private static native char $charAt(String obj, int index) /*-{
    return obj.charCodeAt(index);
  }-*/;

  @JsMethod
  private static native String $subSequence(String obj, int start, int end) /*-{
    return obj.substring(start, end);
  }-*/;

  @JsMethod
  private static native String $substring(String obj, int start, int endIndex) /*-{
    return obj.substring(start, endIndex);
  }-*/;

  @JsMethod
  private static native String $substring(String obj, int start) /*-{
    return obj.substring(start);
  }-*/;

  @JsMethod
  private static native String $trim(String obj) /*-{ return obj.trim(); }-*/;

  @JsMethod
  private static native int $compareTo(String one, String other) /*-{
    if (one == other) {
      return 0;
    }
    return one < other ? -1 : 1;
  }-*/;
}
