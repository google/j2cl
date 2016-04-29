# Google owns the copyright
licenses(["unencumbered"])

load(
    "//:tools/javatests/com/google/j2cl/tools/integration/jsni_converter_test.bzl",
    "jsni_converter_test",
)

jsni_converter_test(
    name = "inner_classes_test",
    # Parent class doesn't contains any native method, so no Parent.native.js file generated
    generated_js_files = [
        "Outer_Inner.native.js",
        "Outer_Inner_InnerInner.native.js",
    ],
    java_files = ["Outer.java"],
    js_test_files = ["inner_classes_test.js"],
)
