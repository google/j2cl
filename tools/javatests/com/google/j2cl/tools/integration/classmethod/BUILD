# Google owns the copyright
licenses(["unencumbered"])

load(
    "//:tools/javatests/com/google/j2cl/tools/integration/jsni_converter_test.bzl",
    "jsni_converter_test",
)

jsni_converter_test(
    name = "class_method_test",
    generated_js_files = ["ClassWithNativeMethod.native.js"],
    java_files = ["ClassWithNativeMethod.java"],
    js_test_files = ["class_method_test.js"],
)
