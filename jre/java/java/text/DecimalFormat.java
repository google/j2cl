package java.text;

import jsinterop.annotations.JsType;

import java.util.HashMap;
import java.util.Map;

import static jsinterop.annotations.JsPackage.GLOBAL;

public class DecimalFormat {

  private final String pattern;

  private final NativeNumberFormat nativeNumberFormat;

  public DecimalFormat(String pattern) {
    this.pattern = pattern;

    int decimalIndex = pattern.indexOf(".");
    int fractionalSize = pattern.length() - decimalIndex - 1;

    Map<String, Object> options = new HashMap<>();
    options.put("minimumFractionDigits", fractionalSize);
    options.put("maximumFractionDigits", fractionalSize);

    nativeNumberFormat = new NativeNumberFormat("en-US", options);
  }

  public String format(Number number) {
    return nativeNumberFormat.format(number);
  }

  @JsType(isNative = true, name = "Intl.NumberFormat", namespace = GLOBAL)
  public static class NativeNumberFormat {

    public NativeNumberFormat(String local, Object options) {
    }

    public native String format(Number value);
  }
}
