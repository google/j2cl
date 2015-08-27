# Google owns the copyright
licenses(["unencumbered"])

load(
    "/third_party/java_src/j2cl/tools/javatests/com/google/j2cl/tools/integration/j2cl_tool_test",
    "jsni_converter_test",
)

jsni_converter_test(
    name = "class_method_test",
    generated_js_files = ["ClassWithNativeMethod.native.js"],
    java_files = ["ClassWithNativeMethod.java"],
    js_test_files = ["class_method_test.js"],
)
