# Google owns the copyright
licenses(["unencumbered"])

load(
    "//:tools/javatests/com/google/j2cl/tools/integration/jsni_converter_test.bzl",
    "jsni_converter_test",
)

jsni_converter_test(
    name = "clinit_test",
    # Parent class doesn't contains any native method, so no Parent.native.js file generated
    generated_js_files = [
        "Main.native.js",
    ],
    java_files = ["Main.java"],
    js_test_files = ["clinit_test.js"],
)
