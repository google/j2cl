package java.util.regex;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;


@JsType(isNative = true, name = "RegExp", namespace = JsPackage.GLOBAL)
class NativeRegExp {
    public NativeRegExp(String regEx) {}

    public native String[] exec(String s);

    public native boolean test(String s);
}