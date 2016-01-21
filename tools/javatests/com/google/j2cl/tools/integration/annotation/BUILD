# Google owns the copyright
licenses(["unencumbered"])

load(
    "//tools:javatests/com/google/j2cl/tools/integration/jsni_converter_test.bzl",
    "jsni_converter_test",
)

jsni_converter_test(
    name = "annotation_test",
    generated_js_files = ["AnnotatedMethod.native.js"],
    java_files = ["AnnotatedMethod.java"],
    js_test_files = ["annotation_test.js"],
    deps = ["//jre/java:gwt-jsinterop-annotations_java_library"],
)
