# Description:
#   j2cl/jre: JRE subset for transpilation.

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load("//build_def:j2cl_source_copy.bzl", "j2cl_source_copy")
load("//build_def:jsni_to_j2cl_converter.bzl", "jsni_to_native_js_bundle")

j2cl_source_copy(
    name = "emul_test_copy",
    srcs = ["//third_party/java_src/gwt/svn/trunk/user:java_emul_test"],
)

j2cl_source_copy(
    name = "java_emul_internal",
    srcs = ["//third_party/java_src/gwt/svn/trunk/user:java_emul_internal"],
    excludes = ["javaemul/internal/EmulatedCharset.java"],
)

jsni_to_native_js_bundle(
    name = "native_js_bundle",
    srcs = [":emul_test_copy"],
    native_srcs = glob(["**/*.native.js"]),
    deps = [":emul_tests_lib_java_library"],
)

TEST_SRCS = glob(["**/*.java"]) + [":emul_test_copy"]

# This runs all GWTTestCases for emul in a pure JRE, this way we can make sure that our tests
# assert the right behaviour for the JRE emulation.
java_test(
    name = "AllEmulTest_Java",
    size = "small",
    srcs = TEST_SRCS + [":java_emul_internal"],
    test_class = "com.google.gwt.emultest.AllTests",
    deps = [
        "//third_party/java/gwt:gwt-jsinterop-annotations",
        "//third_party/java/junit",
    ],
)

load(":j2cl_multi_test.bzl", "j2cl_multi_test")

j2cl_multi_test(
    name = "BigDecimal",
    test_class = "com.google.gwt.emultest.BigDecimalTests",
)

j2cl_multi_test(
    name = "BigInteger",
    test_class = "com.google.gwt.emultest.BigIntegerTests",
)

j2cl_multi_test(
    name = "Collections",
    test_class = "com.google.gwt.emultest.CollectionsTests",
)

j2cl_multi_test(
    name = "Emul",
    test_class = "com.google.gwt.emultest.EmulTests",
)
