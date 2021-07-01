package java.util.regex;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Pattern {

  @JsType(isNative = true, name = "RegExp", namespace = JsPackage.GLOBAL)
  private static class NativeRegExp {
    public NativeRegExp(String regEx) {}

    public native String[] exec(String s);
  }

  public static Pattern compile(String pattern) {
    return compile(pattern, 0);
  }

  public static Pattern compile(String pattern, int flags) {
    return new Pattern(pattern, flags);
  }

  public static boolean matches(String pattern, CharSequence string) {
    return compile(pattern).matcher(string).matches();
  }

  public static String quote(String pattern) {
    throw new UnsupportedOperationException();
  }

  private final String pattern;

  private final int flags;

  private final NativeRegExp nativeRegExp;

  private Pattern(String pattern, int flags) {
    this.pattern = pattern;
    this.flags = flags;
    this.nativeRegExp = new NativeRegExp(pattern);
    if (flags != 0) {
      throw new UnsupportedOperationException("flags not supported");
    }
  }

  public String pattern() {
    return pattern;
  }

  public String toString() {
    return pattern;
  }

  public Matcher matcher(CharSequence string) {
    return new Matcher(this, string, nativeRegExp.exec(string.toString()));
  }

  public int flags() {
    return flags;
  }

  public String[] split(CharSequence string, int limit) {
    return string.toString().split(pattern, limit);
  }

  public String[] split(CharSequence string) {
    return string.toString().split(pattern);
  }

  public Predicate<String> asPredicate() {
    return input -> matcher(input).matches();
  }

  public Stream<String> splitAsStream(CharSequence string) {
    return Arrays.stream(split(string));
  }

}
