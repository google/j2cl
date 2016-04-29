# Description:
#   j2cl/jre: JRE subset for transpilation.

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load("//build_def:j2cl_source_copy.bzl", "j2cl_source_copy")
load("//build_def:j2cl_mirror_from_gwt.bzl", "j2cl_mirror_from_gwt")

j2cl_mirror_from_gwt(
    name = "emul_tests_lib",
    mirrored_files = ["//third_party/java_src/gwt/svn/trunk/user:java_emul_test"],
    deps = [
        "//jre/java:gwt-jsinterop-annotations",
        "//third_party/java/junit",
    ],
)

j2cl_source_copy(
    name = "java_emul_internal",
    srcs = ["//third_party/java_src/gwt/svn/trunk/user:java_emul_internal"],
    excludes = ["javaemul/internal/EmulatedCharset.java"],
)

# This runs all GWTTestCases for emul in a pure JRE, this way we can make sure that our tests
# assert the right behaviour for the JRE emulation.
java_test(
    name = "AllEmulTest_Java",
    size = "small",
    srcs = [
        ":emul_tests_lib_files",
        ":java_emul_internal",
    ],
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
