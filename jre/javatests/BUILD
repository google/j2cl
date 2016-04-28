# Description:
#   j2cl/jre: JRE subset for transpilation.

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load("//build_def:j2cl_source_copy.bzl", "j2cl_source_copy")
load("//jre/javatests:j2cl_multi_test.bzl", "j2cl_multi_test")
load("//build_def:jsni_to_j2cl_converter.bzl", "jsni_to_j2cl_converter")

j2cl_source_copy(
    name = "emul_test_copy",
    srcs = ["//third_party/java_src/gwt/svn/trunk/user:java_emul_test"],
    base_strip_path = "third_party/java_src/gwt/svn/trunk/user/test/",
)

j2cl_source_copy(
    name = "java_emul_internal_copy",
    srcs = ["//third_party/java_src/gwt/svn/trunk/user:java_emul_internal"],
    base_strip_path = "third_party/java_src/gwt/svn/trunk/user/super/com/google/gwt/emul/",
    excludes = ["javaemul/internal/EmulatedCharset.java"],
)

jsni_to_j2cl_converter(
    name = "convert_jsni_emul_j2cl",
    testonly = 1,
    srcs = [
        ":emul_test_copy",
    ],
    excludes = [
        "com/google/gwt/testing/TestUtils.java",
        "java/lang/JsExceptionViolator.java",
        "java/math/BigIntegerViolator.java",
        "java/util/TreeMapViolator.java",
        "java/util/TreeMapViolator.java",
    ],
    deps = [
        ":emul_tests_lib",
        "//jre/java:jre_java_library",
        "//junit/opensource/java:junit_emul_java_library",
    ],
)

TEST_SRCS = [
    "com/google/gwt/core/client/JavaScriptObject.java",
    "com/google/gwt/junit/DoNotRunWith.java",
    "com/google/gwt/junit/Platform.java",
    "com/google/gwt/junit/client/GWTTestCase.java",
    "com/google/gwt/junit/tools/GWTTestSuite.java",
    ":emul_test_copy",
]

# This runs all GWTTestCases for emul in a pure JRE, this way we can make sure that our tests
# assert the right behaviour for the JRE emulation.
java_library(
    name = "emul_tests_lib",
    testonly = 1,
    srcs = TEST_SRCS + [":java_emul_internal_copy"],
    deps = [
        "//third_party/java/gwt:gwt-jsinterop-annotations",
        "//third_party/java/junit",
    ],
)

java_test(
    name = "AllEmulTest_Java",
    size = "small",
    srcs = ["com/google/gwt/emultest/AllTests.java"],
    test_class = "com.google.gwt.emultest.AllTests",
    deps = [
        ":emul_tests_lib",
        "//third_party/java/junit",
    ],
)

j2cl_multi_test(
    name = "BigDecimal",
    srcs = ["com/google/gwt/emultest/BigDecimalTests.java"],
    test_class = "com.google.gwt.emultest.BigDecimalTests",
    deps = [
        ":emul_tests_lib_j2cl",
        "//third_party/java/junit",
    ],
)

j2cl_multi_test(
    name = "BigInteger",
    srcs = ["com/google/gwt/emultest/BigIntegerTests.java"],
    test_class = "com.google.gwt.emultest.BigIntegerTests",
    deps = [
        ":emul_tests_lib_j2cl",
        "//third_party/java/junit",
    ],
)

j2cl_multi_test(
    name = "Collections",
    srcs = ["com/google/gwt/emultest/CollectionsTests.java"],
    test_class = "com.google.gwt.emultest.CollectionsTests",
    deps = [
        ":emul_tests_lib_j2cl",
        "//third_party/java/junit",
    ],
)

j2cl_multi_test(
    name = "Emul",
    srcs = ["com/google/gwt/emultest/EmulTests.java"],
    test_class = "com.google.gwt.emultest.EmulTests",
    deps = [
        ":emul_tests_lib_j2cl",
        "//third_party/java/junit",
    ],
)
