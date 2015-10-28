# Google owns the copyright
licenses(["unencumbered"])

load(
    "/third_party/java_src/j2cl/tools/javatests/com/google/j2cl/tools/integration/j2cl_tool_test",
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
